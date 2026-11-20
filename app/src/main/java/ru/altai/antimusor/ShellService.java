package ru.altai.antimusor;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Shell-служба Shizuku. Исполняется в процессе Shizuku (права shell),
 * поэтому pm disable-user / pm enable работают без root и без ПК.
 * Протокол простой: transact(1) принимает строку-скрипт, возвращает вывод.
 */
public class ShellService extends Binder {

    public ShellService() { }

    public ShellService(Context context) { }

    public void destroy() {
        System.exit(0);
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 1) {
            String cmd = data.readString();
            String out = run(cmd == null ? "" : cmd);
            reply.writeString(out);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    private static String run(String cmd) {
        StringBuilder sb = new StringBuilder();
        try {
            Process p = new ProcessBuilder("sh", "-c", cmd)
                    .redirectErrorStream(true)
                    .start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            int code = p.waitFor();
            sb.append("EXIT ").append(code).append('\n');
        } catch (Throwable t) {
            sb.append("ERR service :: ").append(t).append('\n');
        }
        return sb.toString();
    }
}
