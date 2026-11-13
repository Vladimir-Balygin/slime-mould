package slime;

/**
 * Observer pattern, observer side: an object that wants to hear when a
 * subject changes. The observer never keeps asking "has anything
 * changed?"; the subject calls update() after every change.
 *
 * This is the pull version of the pattern: update() carries no data, and
 * each observer asks its subject for exactly what it needs.
 */
public interface Observer {

    void update();
}
