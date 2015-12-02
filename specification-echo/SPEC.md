# Echo Nukleus

The Echo Nukleus is responsible for echoing data from an inbound stream to the correlated outbound stream.

## Control Commands
The Echo Nukleus is controlled by sending control commands to a memory-mapped file at `[config-dir]/echo/control`.

The following control commands and responses are supported by the Echo Nukleus.

### ERROR response (0x40000000)
Indicates that an error has occurred when attempting to process a command. 

```
ERROR
[long] correlation id
```

### CAPTURE command (0x00000001)
Creates, maps and reads streams from a memory-mapped file at `[config-dir]/echo/streams/[source]`.

```
CAPTURE
[long] correlation id
[string] source nukleus name
```

### CAPTURED response (0x40000001)
Indicates that the CAPTURE command completed successfully. 

```
CAPTURED
[long] correlation id
```

### UNCAPTURE command (0x00000002)
No longer reads streams from the memory-mapped file at `[config-dir]/echo/streams/[source]`.

```
UNCAPTURE
[long] correlation id
[string] source nukleus name
```

### UNCAPTURED response (0x40000002)
Indicates that the UNCAPTURE command completed successfully. 

```
UNCAPTURED
[long] correlation id
```

### ROUTE command (0x00000003)
Maps existing file and writes streams to a memory-mapped file at `[config-dir]/[destination]/streams/echo`.

```
ROUTE
[long] correlation id
[string] destination nukleus name
```

### ROUTED response (0x40000003)
Indicates that the ROUTE command completed successfully. 

```
ROUTED
[long] correlation id
```

### UNROUTE command (0x00000004)
Unmaps and no longer writes streams to the memory-mapped file at `[config-dir]/[destination]/streams/echo`.

```
UNROUTE
[long] correlation id
[string] destination nukleus name
```

### UNROUTED response (0x40000004)
Indicates that the UNROUTE command completed successfully. 

```
UNROUTED
[long] correlation id
```

### BIND command (0x00000011)
Binds incoming streams to be echoed. 

```
BIND
[long] correlation id
[string] source nukleus name
[long] source nukleus reference
```

### BOUND response (0x40000011)
Indicates that the BIND command completed successfully, returning a bind reference. 

```
BOUND
[long] correlation id
[long] bind reference
```

### UNBIND command (0x00000012)
No longer binds incoming streams to be echoed for the previously bound source and source reference.

```
UNBIND
[long] correlation id
[long] bind reference
```

### UNBOUND response (0x40000012)
Indicates that the UNBIND command completed successfully. 

```
UNBOUND
[long] correlation id
```

### PREPARE command (0x00000013)
Prepares to initiate streams to the specified destination and destination reference.

```
PREPARE
[long] correlation id
[string] destination nukleus name
[long] destination nukleus reference
```

### PREPARED response (0x40000013)
Indicates that the PREPARE command completed successfully, returning a prepare reference. 

```
PREPARED
[long] correlation id
[long] prepare reference
```

### UNPREPARE command (0x00000014)
No longer prepares incoming streams from the handler for this prepare reference.

```
UNPREPARE
[long] correlation id
[long] prepare reference
```

### UNPREPARED response (0x40000014)
Indicates that the UNPREPARED command completed successfully. 

```
UNPREPARED
[long] correlation id
```

## Stream Events
The Echo Nukleus describes unidirectional streams of data with the following events.

### RESET event (0x00000000)
Resets the stream as an error condition has occurred.

```
RESET
[long] stream id
```

### BEGIN event (0x00000001)
Indicates the beginning of a new stream.

If the stream identifier is odd, then the bind/prepare reference is required as it is initiating a bidirectional connection.
If the stream identifier is even and non-zero, then the correlating stream identifier of the initiating stream is required instead as this stream represents the opposite direction of an already initiated bidirectional connection.

```
BEGIN
[long] stream id
[long] bind/prepare reference | correlating stream id
```

### DATA event (0x00000002)
Indicates data for an existing stream.

```
DATA
[long] stream id
[bytes] payload
```

### END event (0x00000003)
Indicates the end of an existing stream

```
END
[long] stream id
```
