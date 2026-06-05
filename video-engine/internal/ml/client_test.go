package ml

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestTriggerProcess(t *testing.T) {
	// Create a mock HTTP server to verify the request payload
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/v1/process" {
			t.Errorf("expected path /api/v1/process, got %s", r.URL.Path)
		}
		if r.Method != http.MethodPost {
			t.Errorf("expected POST method, got %s", r.Method)
		}

		var req ProcessRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			t.Errorf("failed to decode request body: %v", err)
		}

		if req.VideoID != "test-video" {
			t.Errorf("expected video_id 'test-video', got %s", req.VideoID)
		}
		if req.AudioKey != "media/test-video/audio.wav" {
			t.Errorf("expected audio_key 'media/test-video/audio.wav', got %s", req.AudioKey)
		}
		if req.FramesPrefix != "media/test-video/frames/" {
			t.Errorf("expected frames_prefix 'media/test-video/frames/', got %s", req.FramesPrefix)
		}

		w.WriteHeader(http.StatusAccepted)
	}))
	defer server.Close()

	// Initialize our ml Client pointing to the mock server
	client := &Client{
		baseURL:    server.URL,
		httpClient: server.Client(),
	}

	err := client.TriggerProcess(context.Background(), "test-video", "media/test-video/audio.wav", "media/test-video/frames/")
	if err != nil {
		t.Errorf("expected no error, got %v", err)
	}
}
