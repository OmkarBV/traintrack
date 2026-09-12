# TrainTrack Postman collection

Two files, both optional to combine:

- `TrainTrack.postman_collection.json` — the requests. Works standalone: every URL, seeded id, and credential it uses falls back to a collection variable with a working default (Postman → select the collection → **Variables** tab to see them).
- `TrainTrack.postman_environment.json` — the same variables, as an actual Environment, for anyone who'd rather use Postman's environment dropdown (top-right) than dig into collection variables. Import it too and select it if you want this view — the login requests write the tokens they capture to *both* places, so it works whichever way you import.

## Use it

1. `docker compose up -d`, then start core-api (`./mvnw -pl traintrack-core-api -am spring-boot:run`, `:8080`) — and audit-service too if you want the "Audit Service" folder to return anything (`:8081`).
2. Import `TrainTrack.postman_collection.json` (File → Import, or drag it in). Import `TrainTrack.postman_environment.json` the same way if you want the environment dropdown too, then select **TrainTrack — Local** from it (top-right of the Postman window).
3. Run the four requests in **Auth** first — Acme Admin, Trainer, Employee, Beta Admin. Everything else depends on the tokens they capture. Easiest way: open the collection's `...` menu → **Run collection** and run the whole thing top to bottom once.

## What's in it

Ten folders, one per concern: Health, Auth, Users, Courses, Enrolments, Bulk Enrolment, Certifications, AI Assistant, Audit Service, and **RBAC & Tenant Isolation Examples** — four requests specifically demonstrating the permission model (an Employee blocked from creating a course or reading someone else's certificates, a Beta Industries admin 404'ing on Acme's data) rather than just the happy path.

A few requests need something attached or configured beyond what a collection file can carry on its own:

- **Bulk Enrolment → Submit Bulk Enrolment (CSV)**: attach `postman/sample-bulk-enrolment.csv` (next to this file) to the `file` form-data field — Postman can't embed an actual file inside a portable collection.
- **AI Assistant** requests need a real `ANTHROPIC_API_KEY` set on core-api, or you'll correctly get a 502 rather than a reply.
- **Enrolments → Create Enrolment** uses a fixed `idempotencyKey` variable on purpose, so you can resend it unchanged and see the *same* enrolment id come back instead of a new one — that's the point of the header, not a mistake. Change that variable to a fresh UUID if you want to create another enrolment.

Full request-by-request notes are in each request's description inside Postman.
