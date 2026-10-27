package com.example.therepairmate.data

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

sealed class BookResult {
    data class Success(val ticketId: String) : BookResult()
    object SlotTaken : BookResult()
}

object Repo {
    /** Configurable technician list (MVP). Move to a `technicians` collection in production. */
    val TECHNICIANS = listOf("Dlamini Thabiso", "Lesego Mogara", "Muvhuso Ndanduleni", "Sibusiso Sindane")

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    val uid: String? get() = auth.currentUser?.uid
    val email: String? get() = auth.currentUser?.email

    suspend fun signIn(email: String, password: String) { auth.signInWithEmailAndPassword(email, password).await() }

    suspend fun signUp(name: String, email: String, password: String) {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user!!
        db.collection("users").document(user.uid).set(
            mapOf(
                "uid" to user.uid, "email" to email, "role" to "customer", "name" to name,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Signs in with a Google ID token. Creates a customer profile on first login and never overwrites an existing role. */
    suspend fun signInWithGoogle(idToken: String) {
        val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user!!
        val ref = db.collection("users").document(user.uid)
        if (!ref.get().await().exists()) {
            ref.set(
                mapOf(
                    "uid" to user.uid, "email" to (user.email ?: ""), "role" to "customer",
                    "name" to (user.displayName ?: ""),
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    /** Creates a customer profile if the signed-in user has none (e.g. older accounts). Never touches an existing role. */
    suspend fun ensureProfile() {
        val user = auth.currentUser ?: return
        val ref = db.collection("users").document(user.uid)
        if (ref.get().await().exists()) return
        ref.set(
            mapOf(
                "uid" to user.uid, "email" to (user.email ?: ""), "role" to "customer",
                "name" to (user.displayName ?: ""),
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email).await() }
    fun signOut() = auth.signOut()

    /** Returns null if the profile is missing; never grants admin by default. */
    suspend fun profile(): Pair<String, String>? {
        val id = uid ?: return null
        val d = db.collection("users").document(id).get().await()
        if (!d.exists()) return null
        return (d.getString("name") ?: "") to (d.getString("role") ?: "customer")
    }

    fun myRepairs(): Flow<List<Repair>> =
        db.collection("repairs").whereEqualTo("uid", uid ?: "").live()
            .map { list -> list.sortedByDescending { it.dateCreated?.seconds ?: Long.MAX_VALUE } }

    fun allRepairs(): Flow<List<Repair>> =
        db.collection("repairs").orderBy("dateCreated", Query.Direction.DESCENDING).live()

    private fun Query.live(): Flow<List<Repair>> = callbackFlow {
        val reg = addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObjects(Repair::class.java) ?: emptyList())
        }
        awaitClose { reg.remove() }
    }

    fun observeRepair(id: String): Flow<Repair?> = callbackFlow {
        val reg = db.collection("repairs").document(id).addSnapshotListener { s, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(s?.toObject(Repair::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun bookedSlotIds(date: String): Set<String> =
        db.collection("slots").whereEqualTo("date", date).get().await().documents.map { it.id }.toSet()

    /** Slot + repair are written in ONE transaction, so two users cannot take the same slot. */
    suspend fun book(
        model: String, type: String, issue: String, diag: Diagnostic,
        est: Estimate, slot: Slot, notes: String
    ): BookResult {
        val user = uid ?: error("Not signed in")
        repeat(5) {
            val ticket = TicketId.generate()
            val slotRef = db.collection("slots").document(slot.id)
            val repairRef = db.collection("repairs").document(ticket)
            val outcome = db.runTransaction { t ->
                if (t.get(slotRef).exists()) return@runTransaction "slot"
                if (t.get(repairRef).exists()) return@runTransaction "collision"
                t.set(slotRef, mapOf(
                    "date" to slot.date, "startTime" to slot.start, "uid" to user,
                    "ticketId" to ticket, "createdAt" to FieldValue.serverTimestamp()
                ))
                t.set(repairRef, mapOf(
                    "ticketId" to ticket, "uid" to user, "deviceModel" to model, "deviceType" to type,
                    "issueDescription" to issue,
                    "diagnosticData" to mapOf("deviceType" to diag.deviceType, "issueType" to diag.issueType, "answers" to diag.answers),
                    "status" to RepairStatus.RECEIVED.key, "techAssigned" to null,
                    "scheduledSlot" to mapOf("date" to slot.date, "startTime" to slot.start, "endTime" to slot.end, "timezone" to SlotGenerator.ZONE.id),
                    "estimatedCost" to mapOf("minimum" to est.min, "maximum" to est.max, "currency" to est.currency),
                    "notes" to notes,
                    "dateCreated" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                ))
                "ok"
            }.await()
            when (outcome) {
                "slot" -> return BookResult.SlotTaken
                "ok" -> return BookResult.Success(ticket)
            }
        }
        error("Could not generate a unique ticket. Try again.")
    }

    suspend fun updateStatus(id: String, s: RepairStatus) {
        db.collection("repairs").document(id)
            .update(mapOf("status" to s.key, "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun assignTech(id: String, tech: String?) {
        db.collection("repairs").document(id)
            .update(mapOf("techAssigned" to tech, "updatedAt" to FieldValue.serverTimestamp())).await()
    }
}