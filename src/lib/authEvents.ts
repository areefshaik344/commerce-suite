/**
 * Cross-module auth event bus — keeps the api client free of zustand imports
 * and avoids circular dependency between authStore <-> apiClient.
 */
type Listener = () => void;
const sessionExpiredListeners = new Set<Listener>();

export const authEvents = {
  onSessionExpired(fn: Listener) {
    sessionExpiredListeners.add(fn);
    return () => sessionExpiredListeners.delete(fn);
  },
  emitSessionExpired() {
    sessionExpiredListeners.forEach((fn) => fn());
  },
};