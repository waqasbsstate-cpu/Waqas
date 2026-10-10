# CLEAR PREMIUM WATER — Android Billing & Ledger

Native Android Studio project for an offline bottled-water billing app. Company details are prefilled in the app/PDF template:
- CLEAR PREMIUM WATER
- Faram Rahimabad, Mingora, Swat
- 03425214489
- PKR (whole rupees)

## Build an APK
1. Install Android Studio (current stable) and the Android SDK Platform 35.
2. Open the `ClearPremiumWater` folder in Android Studio.
3. Allow Gradle sync and install any requested Android SDK components.
4. Run **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

This environment did not have the Android SDK, Gradle executable, or Android platform build tools, so an APK could not be compiled or device-tested here. Do not treat the source archive as an installable APK.

## Implemented in this project
- Local SQLite database; no internet permission or server required.
- Customer create/edit, individual customer ledger, invoice history, and payment history.
- Products list and add-product workflow.
- Customer-specific product rates, automatically loaded when making an invoice; rates can be changed on the invoice and saved for future use.
- Invoice creation with quantity/rate line totals, previous balance snapshot, and payment at creation.
- Separate payment records for later full/partial payments; outstanding calculated as invoices total minus all payments, preventing previous-balance snapshots from being added repeatedly.
- PDF invoice generation and Android share sheet for WhatsApp/printing apps.
- Database backup and restore using Android document picker.
- Dashboard totals and outstanding balances.

## Important limitations / verification status
- Source was reviewed and core arithmetic is independently tested with plain Java; Android compilation and device workflows were not available in this build environment.
- This initial release stores whole PKR amounts (integer rupees), not paisa/decimal prices.
- Product renaming/deactivation UI, invoice deletion/editing, payment allocation to a particular old invoice, CSV export, and automated database migration beyond schema v1 are not included yet.
- PDF files are generated in the app cache and shared. Save/share a copy to keep a permanent invoice file.
- The restore feature replaces the current database. Back up current records before restoring.
- No authentic logo was supplied, so the invoice uses a text-only company header.

## Main workflow
1. Open Customers → Add Customer.
2. Open Products & Rates → choose customer → set that customer's rates.
3. New Invoice → choose customer → add product lines → confirm quantities/rates → enter payment received → Save Invoice & Create PDF.
4. Use the customer's ledger to review invoices and payments or record a later payment.
5. Use Backup Database regularly and store the `.db` file somewhere safe.
