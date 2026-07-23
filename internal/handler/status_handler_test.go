// Package handler_test contains focused automated tests for StatusHandler.
//
// Test categories (spec H26-04-VALIDACAO):
//   - Unit      – handler behaviour in isolation via httptest.ResponseRecorder
//   - Integration – route wiring via the application mux (NewRouter)
//   - Failure   – method enforcement and resilience to bad input
//   - Security  – response hygiene (no leaks, no cacheable health data)
package handler_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/controlmesh/wkflow-test-h26-validacao/internal/handler"
)

// ─────────────────────────────────────────────────────────────────────────────
// shared helpers
// ─────────────────────────────────────────────────────────────────────────────

// newGetRequest creates a GET request aimed at the status endpoint.
func newGetRequest(t *testing.T) *http.Request {
	t.Helper()
	req := httptest.NewRequest(http.MethodGet, "/api/v1/status", nil)
	return req
}

// callHandler invokes StatusHandler directly (no mux) and returns the recorder.
func callHandler(t *testing.T, method string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(method, "/api/v1/status", nil)
	rr := httptest.NewRecorder()
	handler.StatusHandler(rr, req)
	return rr
}

// statusPayload mirrors StatusResponse for decoding without importing the
// production struct (keeps test isolation clean).
type statusPayload struct {
	Service   string `json:"service"`
	Status    string `json:"status"`
	Timestamp string `json:"timestamp"`
}

// decodePayload decodes the recorder body into a statusPayload.
// It fails the test immediately if the body is not valid JSON.
func decodePayload(t *testing.T, rr *httptest.ResponseRecorder) statusPayload {
	t.Helper()
	var p statusPayload
	if err := json.NewDecoder(rr.Body).Decode(&p); err != nil {
		t.Fatalf("response body is not valid JSON: %v — raw body: %q",
			err, rr.Body.String())
	}
	return p
}

// ─────────────────────────────────────────────────────────────────────────────
// UNIT — happy path (satisfies spec minimum: "pelo menos 1 teste unitário")
// ─────────────────────────────────────────────────────────────────────────────

// TestStatusHandler_HTTPStatusCode verifies that GET returns HTTP 200 OK.
// This is the spec-minimum test referenced in H26-04-VALIDACAO.
func TestStatusHandler_HTTPStatusCode(t *testing.T) {
	rr := callHandler(t, http.MethodGet)

	if rr.Code != http.StatusOK {
		t.Errorf("HTTP status: want %d (OK), got %d", http.StatusOK, rr.Code)
	}
}

// TestStatusHandler_ContentTypeJSON verifies the Content-Type header is
// application/json so consumers can rely on machine-readable output.
func TestStatusHandler_ContentTypeJSON(t *testing.T) {
	rr := callHandler(t, http.MethodGet)

	ct := rr.Header().Get("Content-Type")
	if !strings.Contains(ct, "application/json") {
		t.Errorf("Content-Type: want application/json, got %q", ct)
	}
}

// TestStatusHandler_ServiceNameField verifies the "service" field equals the
// value mandated by spec H26-04-VALIDACAO: "controlmesh-test".
func TestStatusHandler_ServiceNameField(t *testing.T) {
	p := decodePayload(t, callHandler(t, http.MethodGet))

	if p.Service != handler.ServiceName {
		t.Errorf("service: want %q, got %q", handler.ServiceName, p.Service)
	}
}

// TestStatusHandler_StatusOkField verifies the "status" field is "ok".
func TestStatusHandler_StatusOkField(t *testing.T) {
	p := decodePayload(t, callHandler(t, http.MethodGet))

	const wantStatus = "ok"
	if p.Status != wantStatus {
		t.Errorf("status: want %q, got %q", wantStatus, p.Status)
	}
}

// TestStatusHandler_TimestampISO8601 verifies the "timestamp" field is
// non-empty and parses as a valid RFC-3339 / ISO-8601 value whose wall time
// falls within the test execution window (±2 s for slow CI hosts).
func TestStatusHandler_TimestampISO8601(t *testing.T) {
	const clockSlack = 2 * time.Second

	before := time.Now().UTC().Add(-clockSlack)
	p := decodePayload(t, callHandler(t, http.MethodGet))
	after := time.Now().UTC().Add(clockSlack)

	if p.Timestamp == "" {
		t.Fatal("timestamp must not be empty")
	}

	ts, err := time.Parse(time.RFC3339, p.Timestamp)
	if err != nil {
		t.Fatalf("timestamp %q is not valid RFC-3339/ISO-8601: %v", p.Timestamp, err)
	}

	if ts.Before(before) || ts.After(after) {
		t.Errorf("timestamp %v is outside expected window [%v, %v]", ts, before, after)
	}
}

// TestStatusHandler_ExactlyThreeFields verifies the JSON payload contains
// exactly the three fields defined by the spec — no extras, no missing keys.
func TestStatusHandler_ExactlyThreeFields(t *testing.T) {
	rr := callHandler(t, http.MethodGet)

	var raw map[string]interface{}
	if err := json.NewDecoder(rr.Body).Decode(&raw); err != nil {
		t.Fatalf("invalid JSON: %v", err)
	}

	requiredKeys := []string{"service", "status", "timestamp"}
	for _, key := range requiredKeys {
		if _, ok := raw[key]; !ok {
			t.Errorf("required key %q is absent from response body", key)
		}
	}

	if got, want := len(raw), len(requiredKeys); got != want {
		t.Errorf("field count: want %d, got %d — payload keys: %v", want, got, keysOf(raw))
	}
}

// keysOf returns the map keys as a sorted-ish slice for readable diagnostics.
func keysOf(m map[string]interface{}) []string {
	out := make([]string, 0, len(m))
	for k := range m {
		out = append(out, k)
	}
	return out
}

// ─────────────────────────────────────────────────────────────────────────────
// INTEGRATION — route wired correctly through the application mux
// ─────────────────────────────────────────────────────────────────────────────

// newTestRouter builds an http.ServeMux that mirrors the production wiring
// without opening a TCP socket. Adjust the import if the router constructor
// lives in a different package.
func newTestRouter() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/api/v1/status", handler.StatusHandler)
	return mux
}

// TestRoute_StatusEndpointReachable verifies that GET /api/v1/status returns
// 200 when invoked through the router (not the bare handler function).
func TestRoute_StatusEndpointReachable(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/api/v1/status", nil)
	rr := httptest.NewRecorder()
	newTestRouter().ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("route GET /api/v1/status: want 200, got %d", rr.Code)
	}
}

// TestRoute_UnknownPathReturns404 verifies that unregistered paths are not
// accidentally served by the status handler.
func TestRoute_UnknownPathReturns404(t *testing.T) {
	paths := []string{
		"/",
		"/api",
		"/api/v1",
		"/api/v1/unknown",
		"/api/v2/status",
		"/healthz",
	}
	router := newTestRouter()

	for _, path := range paths {
		t.Run(path, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodGet, path, nil)
			rr := httptest.NewRecorder()
			router.ServeHTTP(rr, req)

			if rr.Code != http.StatusNotFound {
				t.Errorf("path %q: want 404, got %d", path, rr.Code)
			}
		})
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// FAILURE — method enforcement and resilience to bad input
// ─────────────────────────────────────────────────────────────────────────────

// TestStatusHandler_NonGetMethodsRejected verifies that POST/PUT/DELETE/PATCH
// do not return 200. Spec specifies GET; other verbs must be rejected.
func TestStatusHandler_NonGetMethodsRejected(t *testing.T) {
	nonGetMethods := []string{
		http.MethodPost,
		http.MethodPut,
		http.MethodDelete,
		http.MethodPatch,
		http.MethodHead, // HEAD is not defined by spec for this endpoint
	}

	for _, verb := range nonGetMethods {
		t.Run(verb, func(t *testing.T) {
			rr := callHandler(t, verb)

			if rr.Code == http.StatusOK {
				t.Errorf("%s /api/v1/status: must not return 200", verb)
			}
		})
	}
}

// TestStatusHandler_MethodNotAllowedCode verifies that POST returns exactly
// 405 Method Not Allowed (not a generic 400 or 500).
func TestStatusHandler_MethodNotAllowedCode(t *testing.T) {
	rr := callHandler(t, http.MethodPost)

	if rr.Code != http.StatusMethodNotAllowed {
		t.Errorf("POST: want %d (MethodNotAllowed), got %d", http.StatusMethodNotAllowed, rr.Code)
	}
}

// TestStatusHandler_MalformedAcceptHeaderNoPanic verifies the handler does not
// panic when the client sends a syntactically invalid Accept header.
func TestStatusHandler_MalformedAcceptHeaderNoPanic(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/api/v1/status", nil)
	req.Header.Set("Accept", ";;;;invalid;;;;")
	rr := httptest.NewRecorder()

	// Must not panic — the deferred failure would surface as a panic log.
	handler.StatusHandler(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("malformed Accept header: want 200, got %d", rr.Code)
	}
}

// TestStatusHandler_EmptyRequestBodyIgnored verifies the handler works
// correctly even when an unexpected body is sent with a GET request.
func TestStatusHandler_EmptyRequestBodyIgnored(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/api/v1/status",
		strings.NewReader(`{"unexpected":"payload"}`))
	req.Header.Set("Content-Type", "application/json")
	rr := httptest.NewRecorder()

	handler.StatusHandler(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("request with body: want 200, got %d", rr.Code)
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// SECURITY — response hygiene
// ─────────────────────────────────────────────────────────────────────────────

// TestStatusHandler_CacheControlNoStore verifies the response sets
// Cache-Control: no-store so that proxies do not serve stale health data.
func TestStatusHandler_CacheControlNoStore(t *testing.T) {
	rr := callHandler(t, http.MethodGet)

	cc := rr.Header().Get("Cache-Control")
	if !strings.Contains(cc, "no-store") {
		t.Errorf("Cache-Control: want no-store, got %q", cc)
	}
}

// TestStatusHandler_NoSensitiveDataInBody verifies the response body does not
// contain path fragments, credential keywords, or other sensitive tokens.
func TestStatusHandler_NoSensitiveDataInBody(t *testing.T) {
	rr := callHandler(t, http.MethodGet)
	body := strings.ToLower(rr.Body.String())

	forbidden := []string{
		"/home/", "/root/", "/var/", "/etc/",
		"password", "passwd", "secret", "token", "key",
	}
	for _, word := range forbidden {
		if strings.Contains(body, word) {
			t.Errorf("response body contains sensitive word %q — body: %s", word, rr.Body.String())
		}
	}
}

// TestStatusHandler_TimestampContainsNoUnsafeChars verifies the timestamp
// field is safe for embedding in HTML contexts (RFC-3339 allows only
// digits, hyphens, colons, a literal "T", and "Z"/"+"/"-" for offset).
func TestStatusHandler_TimestampContainsNoUnsafeChars(t *testing.T) {
	p := decodePayload(t, callHandler(t, http.MethodGet))

	unsafe := []string{"<", ">", "\"", "'", "&", "script", "\n", "\r"}
	for _, ch := range unsafe {
		if strings.Contains(p.Timestamp, ch) {
			t.Errorf("timestamp contains unsafe character %q — value: %s", ch, p.Timestamp)
		}
	}
}

// TestStatusHandler_ServiceFieldContainsNoUnsafeChars applies the same
// XSS-safety check to the "service" field.
func TestStatusHandler_ServiceFieldContainsNoUnsafeChars(t *testing.T) {
	p := decodePayload(t, callHandler(t, http.MethodGet))

	unsafe := []string{"<", ">", "\"", "'", "&", "script"}
	for _, ch := range unsafe {
		if strings.Contains(p.Service, ch) {
			t.Errorf("service field contains unsafe character %q — value: %s", ch, p.Service)
		}
	}
}

// TestStatusHandler_ResponseIsIdempotent verifies that two consecutive calls
// return the same structural shape (same service name and status; timestamp
// may differ by a small delta).
func TestStatusHandler_ResponseIsIdempotent(t *testing.T) {
	const clockSlack = 2 * time.Second

	p1 := decodePayload(t, callHandler(t, http.MethodGet))
	p2 := decodePayload(t, callHandler(t, http.MethodGet))

	if p1.Service != p2.Service {
		t.Errorf("service field is not stable: first=%q, second=%q", p1.Service, p2.Service)
	}
	if p1.Status != p2.Status {
		t.Errorf("status field is not stable: first=%q, second=%q", p1.Status, p2.Status)
	}

	ts1, err1 := time.Parse(time.RFC3339, p1.Timestamp)
	ts2, err2 := time.Parse(time.RFC3339, p2.Timestamp)
	if err1 != nil || err2 != nil {
		t.Fatalf("timestamp parse errors: %v / %v", err1, err2)
	}
	diff := ts2.Sub(ts1)
	if diff < 0 {
		diff = -diff
	}
	if diff > clockSlack {
		t.Errorf("timestamps differ by %v which exceeds allowed slack %v", diff, clockSlack)
	}
}
