@proposal @staging_only @not_executed
Feature: Native Free activation and local operational authority
  All scenarios are specifications for a future adapter, not passing integration tests.
  No scenario may target production, Tunas Baru or real customer data.

  Background:
    Given an Owner-authorized isolated staging host, MySQL database and authority
    And two adapter workers share nonce, rate-limit, slot and token-family storage
    And accounts A and B are verified users of fixture tenant X
    And account C belongs to fixture tenant Y
    And device keys K1 and K2 are distinct Android installation keys
    And trial dates and roles come only from the staging authority

  Scenario: Login is not activation
    When A logs in using a valid fresh proof from K1
    Then the response matches TokenPairResponse with authenticated true
    And context identifies the authoritative ST, SR, account, session and key
    And no local operational write is permitted before a valid signed lease

  Scenario Outline: Identity and authority failures never grant a lease
    Given <condition>
    When login or activation is requested
    Then response is <status> with error code <code>
    And no slot, write lease or successful operational UI is granted
    Examples:
      | condition                    | status | code                        |
      | wrong password               | 401    | AUTH_INVALID                |
      | unknown account              | 401    | AUTH_INVALID                |
      | unverified email             | 403    | EMAIL_VERIFICATION_REQUIRED |
      | incomplete registration      | 409    | REGISTRATION_INCOMPLETE     |
      | suspended account            | 403    | ACCOUNT_SUSPENDED           |
      | suspended tenant             | 403    | TENANT_SUSPENDED            |
      | suspended product            | 403    | PRODUCT_SUSPENDED           |
      | inactive subscription        | 403    | SUBSCRIPTION_INACTIVE       |
      | authority timeout            | 503    | CONTROL_PLANE_UNAVAILABLE   |
      | missing trial expiry         | 503    | AUTHORITY_SCHEMA_INVALID    |
      | unknown authority status     | 503    | AUTHORITY_SCHEMA_INVALID    |
      | unknown plan                 | 403    | PLAN_UNSUPPORTED            |

  Scenario: Same tenant cannot activate a second device through another account
    Given X has no operational binding and its trial is active
    When A on K1 and B on K2 request activation concurrently through different workers
    Then exactly one activation succeeds and the other returns 409 ACTIVE_SESSION_EXISTS
    And exactly one tenant POS slot and recorded lease epoch exist
    And concurrent retries do not create another slot

  Scenario: Tenant identity cannot be chosen or substituted
    When A submits a tenant_id, tenant_code, plan or permission override
    Then the adapter returns 400 VALIDATION_ERROR
    And C cannot read or renew X's session or lease using Y's credentials
    And using SR as internal ST cannot select a Room partition

  Scenario: Activation is durable before lease delivery
    Given a valid K1 activation command and idempotency key
    When the response is lost after commit and the worker restarts
    Then retry with the same command key and a fresh proof returns the same bounded result
    And the durable maximum outstanding lease expiry is not shortened or forgotten
    And a signing failure issues no usable unrecorded lease

  Scenario: Three-month trial never resets
    Given central dates describe exactly the platform's three-month trial
    When login, refresh, activation, reinstall and recovery are repeated
    Then trial_started_at and trial_expires_at never change
    And month-end and leap-year fixture dates remain central values, not 90-day client math
    And no mobile adapter creates or extends a trial

  Scenario: Expired user can authenticate read-only
    Given X has an expired trial or paid subscription but identity and roles remain valid
    When login and session revalidation complete
    Then authentication succeeds and access_mode is READ_ONLY
    And historical stock, reports, history and export are available within role privileges
    And no write lease is returned
    And activation or renewal returns 403 READ_ONLY
    And product, stock, cart and checkout mutations are denied without deleting data

  Scenario: Offline Free checkout after legitimate activation
    Given K1 holds a valid server-signed lease, intact key and same-boot trusted time anchor
    When network access is lost and the app force closes and reopens within lease validity
    Then a local Room checkout succeeds atomically within the authorized tenant
    And idempotent retry does not duplicate sale or decrement stock twice
    And no product, stock or transaction payload leaves the device

  Scenario: A control-plane outage does not fabricate or extend permissions
    Given K1 has a previously validated lease ending at E
    When authority becomes unreachable
    Then offline writes may continue only until E under the approved lease
    And no failed request advances E, revision, epoch or trial dates
    And an installation without a verified lease remains write-locked

  Scenario Outline: Lease validity is cryptographically bound
    Given <fault>
    When local code requests an operational mutation
    Then no local write commits
    Examples:
      | fault                                      |
      | bad signature                              |
      | algorithm none or wrong typ                 |
      | unknown kid or untrusted key manifest       |
      | staging issuer in public app                |
      | wrong application audience                  |
      | lease for K2 copied to K1                   |
      | old authorization epoch or revision         |
      | mismatched tenant or product account        |
      | missing required permission                 |
      | lease exp beyond authoritative expiry       |
      | missing or tampered monotonic time anchor   |
      | unknown enum, missing field or invalid JSON |

  Scenario: Clock rollback cannot extend offline operations
    Given K1 has valid trusted server time anchored to elapsedRealtime
    When wall time moves backward or forward
    Then the lease deadline follows monotonic elapsed time
    And reboot or missing boot anchor requires online revalidation for writes
    And expiry at the exclusive deadline rolls back an in-flight Room checkout
    And entitled local history remains retained and read-only

  Scenario: Token theft without device key is insufficient
    Given an attacker copies K1's access or refresh credential
    When the attacker sends no proof or a proof signed by K2
    Then response is 401 DEVICE_PROOF_INVALID or 403 DEVICE_BINDING_MISMATCH
    And no token, lease or recovery authority is granted

  Scenario Outline: Proof substitution or replay is rejected
    Given a proof with <fault>
    When sent to a privileged mobile endpoint
    Then it returns 401 DEVICE_PROOF_INVALID or 401 NONCE_REQUIRED
    And shared replay protection works across worker restart
    Examples:
      | fault                    |
      | reused jti or nonce      |
      | wrong HTTP method        |
      | wrong HTTPS target       |
      | stale iat                |
      | missing access token ath |
      | private JWK fields       |
      | substituted JWK          |

  Scenario: Refresh rotation is atomic and retryable without weakening replay defense
    Given K1 has current refresh token R and command key I
    When the first rotation commits but its response is lost
    Then K1 retry with I and fresh nonce gets the same result within the replay window
    And reuse of R outside that bound retry revokes its family
    And parallel refresh cannot create independent surviving refresh branches
    And restored old ciphertext requires reauthentication instead of granting writes

  Scenario: Stale heartbeat or token expiry is not device abandonment
    Given K1 is offline with a valid outstanding write lease ending at E
    When its heartbeat becomes stale or access or refresh credentials expire
    Then K2 cannot acquire the tenant slot before E
    And legacy recovery or session creation cannot bypass the mobile slot

  Scenario: Explicit recovery reserves transfer without offline overlap
    Given K1 has an outstanding offline lease ending at E
    When K2 reauthenticates to initiate recovery
    Then no revocation occurs before explicit confirmation
    When K2 confirms the captured recovery with fresh proof and the same key
    Then online credentials and renewal on K1 are revoked
    And the new epoch is reserved for K2 with effective_after at least E
    And no new write lease is issued before E
    And after E, login and activation recheck current entitlement before granting K2

  Scenario: Recovery race, cancellation and replay are safe
    Given two competing recovery requests capture the same tenant epoch
    When one confirmation reserves transfer
    Then another stale confirmation returns 409 RECOVERY_STALE
    And retry of the committed command is idempotent
    And canceled or expired recovery does not grant a device or extend trial
    And recovery_id cannot be confirmed by a different installation key

  Scenario: Logout does not promise immediate offline revocation
    Given K1 has a lease ending at E
    When K1 logs out online
    Then its local authorization is cleared and refresh family is revoked
    And the server retains the tenant slot until at least E
    And offline logout never claims server revocation succeeded
    And retained local data is not erased or exposed to an unauthenticated installation

  Scenario: Lost device authorization is not SQLite restoration
    When a device is reinstalled or replaced and its Keystore key is lost
    Then copied ciphertext or lease does not authenticate the replacement
    And explicit recovery is required
    And lost Room data is not claimed recovered from the cloud
    And export or database transfer is outside this auth contract

  Scenario: Free cloud boundaries remain enforced
    Given a valid Free mobile session and lease
    When Free attempts any cloud operational write or DIRECT_CLOUD adapter
    Then existing CLOUD_PLAN_DENIED remains effective
    And no cloud operational credential or driver is included in the Android flow
    And auth, refresh and lease traffic contains no sales, stock or product data

  Scenario: Invalid transport responses fail closed
    When the device receives HTML, invalid JSON, excessive body, unknown version, redirect or 5xx
    Then it never transitions from locked to operational on that response
    And no credential follows a redirect or is logged
    And an existing valid offline lease is evaluated independently

  Scenario: Rate limiting and audit are shared and redacted
    When credential, nonce and recovery attempts exceed configured limits across workers
    Then requests return 429 with Retry-After
    And audit includes request ID, action, epoch and outcome without tokens, passwords or raw proof
    And unknown accounts and wrong passwords have the same public failure envelope

  Scenario: Physical and data regression gate
    Given the exact staged APK and a retained local fixture database
    When Owner tests Panda PRJ-80BT, cutter, restart, offline reprint and safe area
    Then existing receipt bytes, cutter behavior and stored receipt identity remain correct
    And failed or expired checkout changes neither sale, stock nor cart
    And no fixture is promoted to a production tenant
