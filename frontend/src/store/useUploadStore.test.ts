import { describe, it, expect, beforeEach } from 'vitest';
import { useUploadStore } from './useUploadStore';

describe('useUploadStore', () => {
  // Get the initial state to reset before each test
  const initialState = useUploadStore.getState();

  beforeEach(() => {
    // Reset the store before each test run
    useUploadStore.setState(initialState, true);
  });

  it('should initialize with an empty tasks object', () => {
    const state = useUploadStore.getState();
    expect(state.tasks).toEqual({});
  });

  it('should add a task correctly', () => {
    const localId = 'test-id-1';
    const filename = 'test-video.mp4';

    useUploadStore.getState().addTask(localId, filename);

    const state = useUploadStore.getState();
    expect(state.tasks[localId]).toBeDefined();
    expect(state.tasks[localId]).toEqual({
      localId,
      filename,
      progress: 0,
      status: 'UPLOADING'
    });
  });

  it('should update task progress correctly', () => {
    const localId = 'test-id-1';
    const filename = 'test-video.mp4';

    useUploadStore.getState().addTask(localId, filename);
    useUploadStore.getState().updateProgress(localId, 50);

    const state = useUploadStore.getState();
    expect(state.tasks[localId].progress).toBe(50);
    expect(state.tasks[localId].status).toBe('UPLOADING'); // Status should remain unchanged
  });

  it('should update task status correctly', () => {
    const localId = 'test-id-1';
    const filename = 'test-video.mp4';

    useUploadStore.getState().addTask(localId, filename);
    useUploadStore.getState().updateStatus(localId, 'SUCCESS');

    const state = useUploadStore.getState();
    expect(state.tasks[localId].status).toBe('SUCCESS');
    expect(state.tasks[localId].backendId).toBeUndefined();
  });

  it('should update task status and backendId correctly', () => {
    const localId = 'test-id-1';
    const filename = 'test-video.mp4';
    const backendId = 'backend-uuid-1234';

    useUploadStore.getState().addTask(localId, filename);
    useUploadStore.getState().updateStatus(localId, 'SUCCESS', backendId);

    const state = useUploadStore.getState();
    expect(state.tasks[localId].status).toBe('SUCCESS');
    expect(state.tasks[localId].backendId).toBe(backendId);
  });

  it('should handle removing an existing task correctly', () => {
    const localId = 'test-id-1';
    const filename = 'test-video.mp4';

    // Add the task
    useUploadStore.getState().addTask(localId, filename);
    expect(useUploadStore.getState().tasks[localId]).toBeDefined();

    // Remove the task
    useUploadStore.getState().removeTask(localId);
    expect(useUploadStore.getState().tasks[localId]).toBeUndefined();
  });

  it('should do nothing when removing a non-existent task', () => {
    const localId = 'test-id-1';

    expect(useUploadStore.getState().tasks[localId]).toBeUndefined();

    // Remove non-existent task
    useUploadStore.getState().removeTask(localId);

    // State should remain empty
    expect(useUploadStore.getState().tasks).toEqual({});
  });

  it('should update state independently for multiple tasks', () => {
    const task1 = { id: 't1', file: 'file1.mp4' };
    const task2 = { id: 't2', file: 'file2.mp4' };

    const store = useUploadStore.getState();

    // Add multiple tasks
    store.addTask(task1.id, task1.file);
    store.addTask(task2.id, task2.file);

    // Update progress independently
    useUploadStore.getState().updateProgress(task1.id, 20);
    useUploadStore.getState().updateProgress(task2.id, 80);

    // Update status independently
    useUploadStore.getState().updateStatus(task1.id, 'UPLOADING');
    useUploadStore.getState().updateStatus(task2.id, 'SUCCESS', 'backend-t2');

    const state = useUploadStore.getState();

    expect(state.tasks[task1.id]).toEqual({
      localId: task1.id,
      filename: task1.file,
      progress: 20,
      status: 'UPLOADING'
    });

    expect(state.tasks[task2.id]).toEqual({
      localId: task2.id,
      filename: task2.file,
      progress: 80,
      status: 'SUCCESS',
      backendId: 'backend-t2'
    });
  });
});
