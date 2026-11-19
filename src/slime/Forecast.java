package slime;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * The weather from its first tick onwards, for ever.
 *
 * Iterator pattern: Forecast is the collection, the Iterable. It stores
 * only the first weather. There is no array of future weather, because
 * the future has no end. Each Weather is worked out from the one before
 * it, at the moment someone asks for it.
 *
 * Iterable is the socket: it promises one method, iterator(). The
 * simulation asks the forecast "give me your iterator" and then pulls one
 * weather per tick. It never learns how the weather is made, so any other
 * Iterable of weather, such as a List, could be plugged in instead.
 */
public class Forecast implements Iterable<Weather> {

    private final Weather first;

    public Forecast(Weather first) {
        if (first == null) {
            throw new IllegalArgumentException("A forecast needs a first weather");
        }
        this.first = first;
    }

    /**
     * Every weather from the first one on, as an infinite stream.
     *
     * Stream.iterate takes a first value, and a function that makes the
     * next value from the one before: here the method reference
     * Weather::next. Streams are lazy, so a weather is only worked out
     * when something asks the stream for it. An infinite stream is safe
     * as long as its user takes only what it needs: with limit, findFirst,
     * or an iterator that is asked for one weather at a time.
     */
    public Stream<Weather> stream() {
        return Stream.iterate(first, Weather::next);
    }

    /**
     * Give me your iterator. Every call returns a new iterator that starts
     * from the first weather and keeps its own place. A stream can be used
     * only once, so each iterator gets a new stream of its own.
     */
    @Override
    public Iterator<Weather> iterator() {
        return stream().iterator();
    }
}
