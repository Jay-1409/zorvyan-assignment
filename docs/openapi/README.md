# OpenAPI Snapshots for GitHub Pages

This folder stores static OpenAPI JSON files consumed by `docs/index.html`.

Files expected:

- `user-service.json`
- `finance-service.json`

Generate/update them from running local services with:

```bash
./scripts/export-openapi.sh
```
