# Aggregation Server

This system aggregates and distributes weather data in JSON format using a RESTful API. It allows multiple clients to simultaneously GET data from the server.

### Start

Start on port `4567` by default but allow a command-line argument to set the port.

### RESTful API

Using `HTTPServer` to create a server that listens on a port.

```
HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
```

Using `HTTPHandler` to handle GET and PUT requests.

### Synchronization

Using `ReadWriteLock` to synchronize access to the weather data file.

```
private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
```

Allow multiple clients to read the data at the same time.

```
rwLock.readLock().lock();
```

When we reach the code

```
rwLock.writeLock().lock();
```

allows only one client to write the data at a time.

### Lamport Clock

Using `AtomicInteger` to implement the Lamport Clock.

Update the Lamport Clock in the JSON if it's older than the current clock.

```
lamportClock.updateAndGet(localClock ->
  Math.max(receivedClock, localClock) + 1
);
```

Put the Lamport Clock in the JSON.

```
weatherData.put("lamport_clock", lamportClock.get());
```

### JSON

Using `Gson` to convert txt to JSON.

```
Gson gson = new Gson();
String json = gson.toJson(*data*);
```

### Delete JSON

If the client does not send a request for 30 seconds, the server will delete the JSON file.

Get the current time and store it in `lastCommunicationTime`.

```
lastCommunicationTime = System.currentTimeMillis();
```
