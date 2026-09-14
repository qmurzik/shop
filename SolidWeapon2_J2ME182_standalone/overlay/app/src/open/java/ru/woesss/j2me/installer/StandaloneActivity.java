package ru.woesss.j2me.installer;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.playsoftware.j2meloader.MainActivity;
import ru.playsoftware.j2meloader.applist.AppItem;
import ru.playsoftware.j2meloader.applist.AppListModel;
import ru.playsoftware.j2meloader.appsdb.AppRepository;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.FileUtils;

/**
 * Standalone launcher for 3D Solid Weapon 2.
 * Keeps the stock J2ME Loader 1.8.2 runtime/M3G implementation intact.
 */
public class StandaloneActivity extends MainActivity {
    private static final String ASSET_JAR = "solidweapon2.jar";
    private static final String LOCAL_JAR = "solidweapon2-embedded.jar";
    private AppRepository repository;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Let MainActivity initialize the normal J2ME Loader work directory first.
        getWindow().getDecorView().postDelayed(this::prepareAndLaunch, 450);
    }

    private void prepareAndLaunch() {
        try {
            File emulatorDir = new File(Config.getEmulatorDir());
            if (!emulatorDir.exists()) emulatorDir.mkdirs();
            FileUtils.initWorkDir(emulatorDir);

            AppListModel model = new ViewModelProvider(this).get(AppListModel.class);
            repository = model.getAppRepository();
            repository.onWorkDirReady();

            File embedded = new File(getCacheDir(), LOCAL_JAR);
            copyAsset(embedded);

            AppInstaller installer = new AppInstaller(
                    embedded.getAbsolutePath(), Uri.fromFile(embedded), getApplication(), repository);

            Single.<Integer>create(installer::loadInfo)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(status -> handleInstallerStatus(installer, status), this::fatal);
        } catch (Throwable t) {
            fatal(t);
        }
    }

    private void handleInstallerStatus(AppInstaller installer, int status) {
        try {
            if (status == AppInstaller.STATUS_NEW ||
                    status == AppInstaller.STATUS_NEWEST ||
                    status == AppInstaller.STATUS_OLDEST) {
                installAndLaunch(installer);
                return;
            }

            if (status == AppInstaller.STATUS_EQUAL) {
                AppItem app = installer.getExistsApp();
                if (app != null) {
                    launch(app);
                } else {
                    installAndLaunch(installer);
                }
                return;
            }

            // For an embedded JAR these statuses should not normally occur.
            installAndLaunch(installer);
        } catch (Throwable t) {
            fatal(t);
        }
    }

    private void installAndLaunch(AppInstaller installer) {
        Single.<Integer>create(installer::install)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    if (status == AppInstaller.STATUS_SUCCESS) {
                        AppItem app = installer.getExistsApp();
                        if (app != null) {
                            launch(app);
                            return;
                        }
                    }
                    fatal(new IllegalStateException("MIDlet install status: " + status));
                }, this::fatal);
    }

    private void launch(AppItem app) {
        Config.startApp(this, app.getTitle(), app.getPathExt(), false);
    }

    private void copyAsset(File out) throws Exception {
        try (InputStream in = getAssets().open(ASSET_JAR);
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[32768];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
        }
    }

    private void fatal(Throwable t) {
        t.printStackTrace();
        Toast.makeText(this, "Solid Weapon 2: " + t.getClass().getSimpleName() + ": " +
                String.valueOf(t.getMessage()), Toast.LENGTH_LONG).show();
    }
}
