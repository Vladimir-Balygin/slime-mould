package slime;

/**
 * Observer pattern, subject side: an object that others can watch.
 * The method names follow the Head First version of the pattern.
 */
public interface Subject {

    void registerObserver(Observer observer);

    void removeObserver(Observer observer);

    void notifyObservers();
}
