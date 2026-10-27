# Firebase setup for The Repairmate

Project: repairmate-bac33 (package com.example.therepairmate)

1. Authentication -> Sign-in method: enable **Email/Password** (and **Google** if you use "Continue with Google").
2. Firestore Database -> Create database (production mode). Then Rules tab -> paste `firestore.rules` -> Publish.
3. Every account created in the app is saved to Authentication, and its profile is saved in Firestore `users/{uid}`
   (uid, email, name, role = "customer", createdAt, updatedAt).

## Admin login
There is no built-in admin password. An admin is any account whose Firestore profile has role = "admin".
1. In the app, sign up with the email/password you want to use as admin (e.g. admin@yourdomain.com + a strong password).
2. Firebase console -> Firestore -> users -> open the document for that account (find it by the email field).
3. Edit the field `role` from "customer" to "admin" and save.
4. Sign out and sign in again with that email and password. You will land on the Admin dashboard.
To make someone else an admin, repeat steps 2-3 for their document. Customers cannot change their own role (blocked by the rules).
