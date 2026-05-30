package api

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestIsValidVideoID(t *testing.T) {
	tests := []struct {
		id    string
		valid bool
	}{
		{"valid-uuid-1234", true},
		{"valid_id_with_underscores", true},
		{"12345", true},
		{"", false},
		{"../traversal", false},
		{"/absolute/path", false},
		{"spaces in id", false},
		{"id_with_special_#", false},
	}

	for _, tt := range tests {
		if got := isValidVideoID(tt.id); got != tt.valid {
			t.Errorf("isValidVideoID(%q) = %v; want %v", tt.id, got, tt.valid)
		}
	}
}

func TestHandleProcess_Validation(t *testing.T) {
	// Создаем тестовый сервер с пустыми клиентами
	server := &Server{
		mux: http.NewServeMux(),
	}
	server.mux.HandleFunc("POST /process", server.handleProcess)

	tests := []struct {
		name       string
		payload    map[string]string
		wantStatus int
	}{
		{
			name: "Valid request",
			payload: map[string]string{
				"video_id": "valid-id",
				"s3_path":  "bucket/video.mp4",
			},
			wantStatus: http.StatusAccepted, // Т.к. дальше уйдет в асинхронный процесс
		},
		{
			name: "Invalid video_id (traversal)",
			payload: map[string]string{
				"video_id": "../invalid-id",
				"s3_path":  "bucket/video.mp4",
			},
			wantStatus: http.StatusBadRequest,
		},
		{
			name: "Missing video_id",
			payload: map[string]string{
				"s3_path": "bucket/video.mp4",
			},
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			body, _ := json.Marshal(tt.payload)
			req := httptest.NewRequest(http.MethodPost, "/process", bytes.NewBuffer(body))
			w := httptest.NewRecorder()

			server.handleProcess(w, req)

			if w.Code != tt.wantStatus {
				t.Errorf("handleProcess status = %d; want %d", w.Code, tt.wantStatus)
			}
		})
	}
}
