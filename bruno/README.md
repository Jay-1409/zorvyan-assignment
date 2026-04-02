# Bruno Collection

## How to use
1. Open `bruno/` as a collection in Bruno.
2. Select environment: `local`.
3. Run one of the login requests from `user-service/`.
4. Copy `accessToken` from response and paste into environment variable `accessToken`.
5. Run the secured requests.

## Base URLs
- `userBaseUrl`: `http://localhost:8081`
- `financeBaseUrl`: `http://localhost:8082`

## Notes
- `userId` and `recordId` are environment variables used by path-based requests.
- Update `page`, `size`, and query parameters as needed for testing.
