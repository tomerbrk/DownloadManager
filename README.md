
Chunk.java - Used to deliver chunks of data from downloading threads to writing thread
DownloadableMetada.java - Describes the download status, the metadata involved in the download process and its maintencate during runtime. Also upon termination, other threads terminate (constantly check download status).
FileWriter.java - Runnable class that writes downloaded data to destination local file. Upon writinig invokes metadata update
HttpRangeGetter.java - Runnable class that downloads a specific range of bytes from a url, then transfers the data to a queue to be written to disk
IdcDm.class - Main class. Initiates all objects involved and starts all threads
Range.class - Describes a range of bytes. Used for metadata handling and range downloading
RangeLimiter.class - Runnable class to limit download rate using a token bucket
TokenBucket.class - Describes a synchronized token bucket for rate lilmiting
