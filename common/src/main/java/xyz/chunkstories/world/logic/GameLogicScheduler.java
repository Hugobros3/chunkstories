//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.plugin.ChunkStoriesPlugin;
import xyz.chunkstories.api.plugin.Scheduler;

public class GameLogicScheduler implements Scheduler {

	private static final Logger logger = LoggerFactory.getLogger("scheduler");

	public Logger logger() {
		return logger;
	}

	private List<ScheduledTask> scheduledTasks = new ArrayList<ScheduledTask>();

	void runScheduledTasks() {
		try {
			scheduledTasks.removeIf(ScheduledTask::etc);
		} catch (Throwable t) {
			logger().error(t.getMessage());
			t.printStackTrace();
		}
	}

	@Override
	public void scheduleSyncRepeatingTask(ChunkStoriesPlugin p, Runnable runnable, long delay, long period) {
		scheduledTasks.add(new ScheduledTask(runnable, delay, period));
	}

	class ScheduledTask {

		ScheduledTask(Runnable runnable, long delay, long period) {
			this.runnable = runnable;
			this.delay = delay;
			this.period = period;
		}

		Runnable runnable;
		long delay;
		long period;

		// Returns true when it's no longer going to run
		boolean etc() {
			if (--delay > 0)
				return false;

			runnable.run();

			if (period > 0)
				delay = period;
			else
				return true;

			return false;
		}
	}

}