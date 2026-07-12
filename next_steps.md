# MTD Submitter — Next Steps & Integration Guide

This guide outlines the remaining tasks, configurations, and HMRC compliance steps needed to launch the application.

---

## 1. Google Cloud Run Configuration Checklist

Now that the app is deployed to Cloud Run, you need to configure the environment variables for your database and third-party APIs.

### Environment Variables to Configure:
Go to your **Google Cloud Run Console** > **`mtd-submitter`** > **Edit & Deploy New Revision** > **Variables & Secrets** and set:

| Variable | Example Value | Description |
|----------|---------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production database and logging configurations. |
| `DATABASE_URL` | `jdbc:postgresql://ep-host.neon.tech/neondb?sslmode=require` | Your PostgreSQL connection string (must have `jdbc:` prefix). |
| `DATABASE_USERNAME` | `neondb_owner` | Database user. |
| `DATABASE_PASSWORD` | `your_db_password` | Database password. |
| `JASYPT_PASSWORD` | `some-secure-encryption-key` | Used to encrypt NINO, UTR, and OAuth tokens at rest. |
| `HMRC_CLIENT_ID` | `your-hmrc-client-id` | Client ID from the HMRC Developer Hub. |
| `HMRC_CLIENT_SECRET` | `your-hmrc-client-secret` | Client Secret from the HMRC Developer Hub. |
| `STRIPE_API_KEY` | `sk_test_...` | Stripe secret API key. |
| `STRIPE_PRICE_ID` | `price_...` | Stripe product price ID for subscription billing. |
| `STRIPE_WEBHOOK_SECRET` | `whsec_...` | Stripe webhook signing secret (used to verify subscription updates). |

---

## 2. HMRC Developer Hub & Sandbox Registration

To test submissions against HMRC’s sandbox APIs, you must register your application.

1. Go to the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/).
2. Log in (or create a developer account).
3. Go to **Applications** > **Add an application**.
4. Enter application name (e.g., `MTD Submitter`).
5. Under **Redirect URIs**, add your production URL callback:
   * `https://YOUR-CLOUD-RUN-URL.run.app/callback/hmrc`
   * *For local development testing, you can also add:* `http://localhost:8080/callback/hmrc`
6. Copy your **Client ID** and **Client Secret** and add them to your Cloud Run environment variables or local environment.
7. Go to **Manage API subscriptions** in your app settings on the hub and subscribe to:
   * **Income Tax (Self Assessment) MTD** APIs
   * **Obligations** APIs

---

## 3. Remaining Development Milestones (Weeks 5-6)

Below are the detailed specifications for the remaining features to be built.

### Milestone 1: Final Declaration (Annual Crystallisation)
* **API Calls:** Map the `HmrcApiService.triggerTaxCalculation` and `getTaxCalculation` endpoints.
* **Controller:** Create `FinalDeclarationController.java` to fetch the final tax estimation.
* **Views:** Build `declaration/review.html` showing a breakdown of the total tax due, allowances, and a final "Crystallise & Submit" button to send the annual return.

### Milestone 2: Stripe Billing Integration
* **Stripe SDK:** Use `com.stripe:stripe-java` (already configured in `pom.xml`).
* **Checkout:** Create a subscription checkout flow at `/subscribe` redirecting to Stripe Checkout.
* **Webhook:** Create a secure endpoint `/webhooks/stripe` to handle events:
  * `customer.subscription.deleted` (mark user account as `EXPIRED`)
  * `invoice.payment_succeeded` (mark user account as `ACTIVE`)
* **Access Guard:** Implement a Spring Interceptor to redirect users to `/subscribe` if their trial has expired.

### Milestone 3: CSV Import
* **CSV Reader:** Use `opencsv` (already in `pom.xml`) to parse uploads at `/import`.
* **Mapping:** Parse columns (Date, Description, Amount, Category) and insert records directly into `income_records` or `expense_records`.

### Milestone 4: Email Reminders
* **Schedule:** Add a cron job or scheduled task (`@Scheduled` in Spring) running once a week.
* **Logic:** Scan `QuarterlyPeriodRepository` for open obligations due in the next 14 days and send reminder emails using Resend/Spring Mail.

---

## 4. How to Run Locally

If you need to test changes locally:

1. Start your local database:
   ```bash
   docker-compose up -d
   ```
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```
3. Run test suites:
   ```bash
   mvn test
   ```
