package rvmonitorrt.test;

import rvmonitorrt.RVMTimer;
import rvmonitorrt.concurrent.RVMLock;
import rvmonitorrt.concurrent.RVMNameStone;

public class RVMLockTest {

	static Thread daemon = new Thread() {
		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {

				}
			}
		}
	};

	public static void main(String args[]) {
		RVMTimer timer_1 = new RVMTimer();
		RVMTimer timer_2 = new RVMTimer();

		daemon.setDaemon(true);
		daemon.start();

		RVMLock moplock = new RVMLock();

		timer_1.start();
		for (int i = 0; i < 100; i++) {
			TestThread1 thread = new TestThread1(moplock);
			thread.start();
			try {
				thread.join();
			} catch (Exception e) {

			}
		}
		timer_1.end();

		timer_2.start();
		for (int i = 0; i < 100; i++) {
			TestThread2 thread = new TestThread2();
			thread.start();
			try {
				thread.join();
			} catch (Exception e) {

			}
		}
		timer_2.end();

		System.out.println("mop: " + timer_1.getElapsedMicroTime());
		System.out.println("java: " + timer_2.getElapsedMicroTime());
	}
}

class TestThread1 extends Thread {
	RVMLock moplock;

	TestThread1(RVMLock moplock) {
		this.moplock = moplock;
	}

	static void doSomething() {
		int s = 0;
		s = 1;

		int k = s;
		k = k + s;
	}

	public void run() {
		for (int i = 0; i < 100000; i++) {
			RVMNameStone stone = moplock.lock();

			doSomething();

			stone.tag = false;
		}
	}
}

class TestThread2 extends Thread {
	Object lock = new Object();

	TestThread2() {
	}

	static void doSomething() {
		int s = 0;
		s = 1;

		int k = s;
		k = k + s;
	}

	public void run() {
		for (int i = 0; i < 100000; i++) {
			synchronized (lock) {
				doSomething();
			}
		}
	}
}