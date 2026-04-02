# Zorvyn Assignment Improvement TODO

Priority-ordered checklist to push this project to a 9+/10 evaluation score.

- [x] 1. Rewrite `README.md` to be evaluation-ready.
  Include system overview, architecture, RBAC, data flow, Redis strategy, DB design, assumptions, tradeoffs, run guide, API guide, and sample payloads.

- [x] 2. Harden Redis serialization strategy.
  Implemented stable typed serialization and moved to versioned cache names (`dashboardSummaryV2`, `recordByIdV2`) to avoid stale-key type conflicts.

- [x] 3. Align soft-delete and uniqueness behavior in `user-service`.
  Added Flyway migration to replace hard unique constraints with active-user-only unique indexes and updated entity mapping.

- [x] 4. Fix and harden `run.sh`.
  Fixed DB username default bug and added command + Oracle/Redis connectivity preflight checks with explicit failure output.

- [x] 5. Add automated tests for critical paths.
  Added unit + WebMvc tests for RBAC, admin safety business rules, and global error policy.

- [x] 6. Add integration tests for key runtime flows.
  Added `@SpringBootTest` coverage for login->authenticated access (`user-service`), DB-backed finance CRUD, and dashboard cache hit/evict behavior.
- [x] 7. Add explicit negative authorization tests.
  Added controller tests proving forbidden role access paths return denial responses.

- [ ] 8. Add end-to-end API verification flow.
  Provide a deterministic sequence (Bruno or script) to verify all mandatory assignment features.

- [ ] 9. Standardize error handling policy across services.
  Partially done: both services now hide internal error details and map security method-denial to `403`; correlation id logging is still pending.

- [ ] 10. Improve container/runtime reliability documentation.
  Add clear local-vs-docker config matrix and startup troubleshooting guidance.

- [ ] 11. Strengthen architecture artifact for reviewability.
  Add failure modes, cache consistency guarantees, and request lifecycle narrative to architecture docs.

- [x] 12. Reduce cross-service duplication with a shared module.
  Extracted common error DTOs and `PagedResponse` into the new `common` Maven module and rewired both microservices to use it.
