package xyz.chunkstories.client;

import xyz.chunkstories.task.WorkerThreadPool;

public class ClientTasksPool extends WorkerThreadPool {

    final ClientImplementation client;

    public ClientTasksPool(ClientImplementation client, int threadsCount) {
        super(threadsCount);
        this.client = client;
    }

    public ClientImplementation getClient() {
        return client;
    }
}
