package essentials.internal.thread;

import essentials.internal.CrashReport;
import essentials.internal.Log;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static essentials.Main.perm;
import static essentials.Main.root;

public class PermissionWatch implements Runnable {
    WatchKey watchKey;
    WatchService watchService;
    Path path;

    public PermissionWatch() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.path = Paths.get(root.absolutePath());
            path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.OVERFLOW);
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                watchKey = watchService.take();
                Thread.sleep(50);

                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent<?> event : events) {
                    WatchEvent.Kind<?> kind = event.kind();
                    String paths = ((Path) event.context()).getFileName().toString();
                    if (paths.equals("permission_user.hjson") || paths.equals("permission.hjson")) {
                        if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                            perm.reload(false);
                            if (!perm.isUse) {
                                perm.isUse = true;
                                perm.update();
                            } else {
                                Log.info("system.perm.updated");
                            }
                        }
                    }
                    /*if(kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        System.out.println("created something in directory");
                    }else if(kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        System.out.println("delete something in directory");
                    }else if(kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                        System.out.println("modified something in directory");
                    }else if(kind.equals(StandardWatchEventKinds.OVERFLOW)) {
                        System.out.println("overflow");
                    }else {
                        System.out.println("hello world");
                    }*/
                }

                if (!watchKey.reset()) {
                    try {
                        watchService.close();
                        break;
                    } catch (IOException e) {
                        new CrashReport(e);
                    }
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception ignored) {
            }
        }
    }
}
