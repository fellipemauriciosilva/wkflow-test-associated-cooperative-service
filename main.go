// Command controlmesh-test starts the HTTP server for spec H26-04-VALIDACAO.
package main

import (
	"log"
	"net/http"

	"github.com/controlmesh/wkflow-test-h26-validacao/internal/handler"
)

func main() {
	mux := NewRouter()

	addr := ":8080"
	log.Printf("level=info msg=\"starting server\" addr=%s", addr)

	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("level=fatal msg=\"server exited\" error=%v", err)
	}
}

// NewRouter builds and returns the application ServeMux.
// Exported so integration tests can obtain a fully wired router without
// starting a real TCP listener.
func NewRouter() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("/api/v1/status", handler.StatusHandler)
	return mux
}
