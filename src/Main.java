import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class Main {
    private static Semaphore semaphore = new Semaphore(3, true), semaphore2 = new Semaphore(1, true);
    private static CountDownLatch countDownLatch = new CountDownLatch(10);
    private static Server server = new Server();
    private static Uploader uploader = new Uploader(server);
    public static Downloader[] downloaders = newDowloaders();
    private synchronized static Downloader[] newDowloaders(){
        Downloader[] downloaders = new Downloader[10];
        for (int i = 0; i < downloaders.length; i++) {
            downloaders[i] = new Downloader(server, semaphore, countDownLatch);
            downloaders[i].setName(downloaders[i].getClass().getSimpleName() + "№" + (i + 1));
        }
        return downloaders;
    }
    public static void main(String[] args) {
        for (int i = 0; i < downloaders.length; i++) {
            downloaders[i].start();
        }
        uploader.start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Файл был удален из сервера.");
    }
}

class Uploader extends Thread {
    Server server;

    public Uploader(Server server) {
        this.server = server;
    }

    @Override
    public synchronized void run() {
        System.out.println("Файл начел загружаться на сервер.");
        for (int i = 5; i < 100; i += 5) {
            server.file = server.file + 20;
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Файл загружается на сервер " + i + "%...");
        }
        System.out.println("Файл загрузился на сервер.");
        ///Проблемы с след. учатскам кода.
        // notifyAll();
        // Ничего не происходит. Downloaders так и ждут команды notify.
        // -------------------------
        // Main.dowloaders.notifyAll();
        // Когда программа доходит до этого кода выдает след. ошибку: IllegalMonitorStateException
        // -------------------------
        // synchronized (Main.downloaders){
        //    Main.downloaders.notifyAll();
        //        }
        // Ничего не происходит как и в прошлый раз.
        ///
            for (int i = 0; i < Main.downloaders.length; i++) {
                synchronized (Main.downloaders[i]) {
                    Main.downloaders[i].notify();
                }
            }
        }
}

    class Downloader extends Thread {
        Semaphore semaphore;
        CountDownLatch countDownLatch;
        Server server;

        public Downloader(Server server, Semaphore semaphore, CountDownLatch countDownLatch) {
            this.server = server;
            this.semaphore = semaphore;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public synchronized void run() {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (server.file != 0) {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(getName() + " начинает скачивать файл.");
                for (int i = 20; i < 100; i += 20) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(getName() + " скачал файл на " + i + "%...");
                }
                System.out.println(getName() + " скачал файл.");
                countDownLatch.countDown();
                semaphore.release();
            }
        }
    }

    class Server {
        public int file;
    }
