package ru.altai.antimusor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final String SAFE =
            "com.glance.internet com.heytap.browser com.heytap.market com.heytap.themestore " +
            "com.heytap.cloud com.heytap.music com.heytap.gamecenter com.nearme.gamecenter " +
            "com.coloros.gamespace com.coloros.gamespaceui com.heytap.quicksearchbox " +
            "com.oppo.quicksearchbox com.coloros.assistantscreen com.oplus.assistantscreen " +
            "com.heytap.hotapps com.oplus.logkit com.oplus.logtrial com.facebook.system " +
            "com.facebook.appmanager com.facebook.services com.netflix.partner.activation";

    private static final String EXTRA =
            "com.google.android.googlequicksearchbox com.oplus.appdetail";

    private static final int REQ = 1001;

    private TextView statusTitle, statusText, logView;
    private LinearLayout guideBox;
    private Button permBtn, bigBtn, scanBtn, restoreBtn, extraBtn, checkBtn;
    private boolean ready = false, busy = false;

    private final Shizuku.OnBinderReceivedListener received = () -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderDeadListener dead = () -> runOnUiThread(this::refresh);
    private final Shizuku.OnRequestPermissionResultListener permResult =
            (rq, gr) -> runOnUiThread(this::refresh);

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF0B1020);
        buildUi();
        Shizuku.addBinderReceivedListenerSticky(received);
        Shizuku.addBinderDeadListener(dead);
        Shizuku.addRequestPermissionResultListener(permResult);
        refresh();
        log("Готов к работе. Активируй Shizuku и жми большую кнопку.");
    }

    @Override protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(received);
        Shizuku.removeBinderDeadListener(dead);
        Shizuku.removeRequestPermissionResultListener(permResult);
        super.onDestroy();
    }

    /* ---------------- интерфейс ---------------- */

    private int dp(int v) {
        return (int) (getResources().getDisplayMetrics().density * v + 0.5f);
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setLineSpacing(dp(3), 1f);
        return t;
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(primary ? 17 : 14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setPadding(dp(12), dp(12), dp(12), dp(12));
        if (primary) {
            GradientDrawable g = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF22D3EE, 0xFF5B6CFF});
            g.setCornerRadius(dp(16));
            b.setBackground(g);
            b.setTextColor(0xFF061024);
        } else {
            b.setBackground(round(0xFF161E3E, 14, 0xFF2C3768));
            b.setTextColor(0xFFDDE4FF);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                primary ? dp(66) : dp(52));
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(round(0xFF121935, 18, 0xFF2A3462));
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        l.setLayoutParams(lp);
        return l;
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(0xFF0B1020);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(28));
        sv.addView(root);

        TextView title = text("АНТИМУСОР", 27, 0xFFEEF2FF, true);
        title.setLetterSpacing(0.14f);
        root.addView(title);
        root.addView(text("realme C71 · без root · всё обратимо", 13, 0xFF93A0CC, false));

        LinearLayout st = card();
        statusTitle = text("Проверяю Shizuku…", 16, 0xFFEEF2FF, true);
        statusText = text("", 13, 0xFFA9B4DC, false);
        st.addView(statusTitle);
        st.addView(statusText);

        permBtn = button("Разрешить доступ к Shizuku", false);
        permBtn.setOnClickListener(v -> {
            try { Shizuku.requestPermission(REQ); }
            catch (Throwable t) { toast("Shizuku не запущена"); }
        });
        st.addView(permBtn);

        guideBox = new LinearLayout(this);
        guideBox.setOrientation(LinearLayout.VERTICAL);
        guideBox.addView(text(
                "Как включить (один раз, без ПК):\n" +
                "1. Установи приложение Shizuku.\n" +
                "2. Настройки → О телефоне → 7 раз тапни «Номер сборки».\n" +
                "3. Для разработчиков → включи «Отладка по Wi-Fi».\n" +
                "4. В Shizuku нажми «Сопряжение», введи код и порт из\n" +
                "    «Отладка по Wi-Fi → Подключить с кодом».\n" +
                "5. В Shizuku нажми «Запуск».\n" +
                "6. Вернись сюда и нажми «Проверить».\n" +
                "После перезагрузки телефона Shizuku нужно запустить снова.",
                13, 0xFFA9B4DC, false));
        Button b1 = button("Установить Shizuku", false);
        b1.setOnClickListener(v -> open("market://details?id=moe.shizuku.privileged.api",
                "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"));
        Button b2 = button("Открыть «Для разработчиков»", false);
        b2.setOnClickListener(v -> intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        Button b3 = button("Открыть Shizuku", false);
        b3.setOnClickListener(v -> launch("moe.shizuku.privileged.api"));
        checkBtn = button("Проверить ещё раз", false);
        checkBtn.setOnClickListener(v -> { refresh(); toast(ready ? "Готово!" : "Shizuku ещё не готова"); });
        guideBox.addView(b1); guideBox.addView(b2); guideBox.addView(b3); guideBox.addView(checkBtn);
        st.addView(guideBox);
        root.addView(st);

        bigBtn = button("ОТКЛЮЧИТЬ ВСЁ ОДНОЙ КНОПКОЙ", true);
        bigBtn.setOnClickListener(v -> confirmMain());
        root.addView(bigBtn);

        scanBtn = button("Проверить список (ничего не менять)", false);
        scanBtn.setOnClickListener(v -> exec(scanScript(SAFE + " " + EXTRA), "Проверка списка"));
        root.addView(scanBtn);

        restoreBtn = button("Вернуть всё как было", false);
        restoreBtn.setOnClickListener(v -> exec(restoreScript(SAFE + " " + EXTRA), "Восстановление"));
        root.addView(restoreBtn);

        extraBtn = button("Дополнительно: лента Google (осторожно)", false);
        extraBtn.setOnClickListener(v -> confirmExtra());
        root.addView(extraBtn);

        LinearLayout man = card();
        man.addView(text("Ручные тумблеры рекламы", 16, 0xFFEEF2FF, true));
        man.addView(text("Это реклама внутри приложений, которые мы оставляем. Открой настройки и выключи «рекомендации».", 13, 0xFFA9B4DC, false));
        Button m1 = button("Менеджер телефона", false);
        m1.setOnClickListener(v -> appInfo("com.coloros.phonemanager"));
        Button m2 = button("Маркет приложений", false);
        m2.setOnClickListener(v -> appInfo("com.heytap.market"));
        Button m3 = button("Браузер", false);
        m3.setOnClickListener(v -> appInfo("com.heytap.browser"));
        Button m4 = button("Все приложения", false);
        m4.setOnClickListener(v -> intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        Button m5 = button("Настройки телефона", false);
        m5.setOnClickListener(v -> intent(Settings.ACTION_SETTINGS));
        man.addView(m1); man.addView(m2); man.addView(m3); man.addView(m4); man.addView(m5);
        root.addView(man);

        LinearLayout lg = card();
        lg.addView(text("Журнал", 16, 0xFFEEF2FF, true));
        logView = text("", 11, 0xFF9FB0E0, false);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setMinHeight(dp(180));
        logView.setTextIsSelectable(true);
        lg.addView(logView);
        Button clr = button("Очистить журнал", false);
        clr.setOnClickListener(v -> logView.setText(""));
        lg.addView(clr);
        root.addView(lg);

        root.addView(text("Отключается только для текущего пользователя. Системный раздел не трогается, обновления и гарантия целы.", 12, 0xFF7C88B4, false));
        setContentView(sv);
    }

    /* ---------------- состояние ---------------- */

    private void refresh() {
        boolean ping = false, granted = false;
        try { ping = Shizuku.pingBinder(); } catch (Throwable ignored) {}
        if (ping) {
            try { granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
            catch (Throwable ignored) {}
        }
        ready = ping && granted;

        if (ready) {
            statusTitle.setText("✅ Всё готово");
            statusTitle.setTextColor(0xFF3CE8B0);
            statusText.setText("Shizuku работает. Жми большую кнопку.");
        } else if (ping) {
            statusTitle.setText("⚠ Нужно разрешение");
            statusTitle.setTextColor(0xFFFFB648);
            statusText.setText("Shizuku запущена, но не разрешила доступ этому приложению.");
        } else {
            statusTitle.setText("⛔ Shizuku не запущена");
            statusTitle.setTextColor(0xFFFF5C93);
            statusText.setText("Выполни шаги ниже — это делается один раз, без компьютера.");
        }
        guideBox.setVisibility(ping ? View.GONE : View.VISIBLE);
        permBtn.setVisibility(ping && !granted ? View.VISIBLE : View.GONE);
        setBusy(busy);
    }

    private void setBusy(boolean b) {
        busy = b;
        bigBtn.setEnabled(ready && !b);
        scanBtn.setEnabled(ready && !b);
        restoreBtn.setEnabled(ready && !b);
        extraBtn.setEnabled(ready && !b);
        bigBtn.setAlpha(ready && !b ? 1f : 0.4f);
        scanBtn.setAlpha(ready && !b ? 1f : 0.4f);
        restoreBtn.setAlpha(ready && !b ? 1f : 0.4f);
        extraBtn.setAlpha(ready && !b ? 1f : 0.4f);
        if (b) bigBtn.setText("Работаю…");
        else bigBtn.setText("ОТКЛЮЧИТЬ ВСЁ ОДНОЙ КНОПКОЙ");
    }

    /* ---------------- диалоги ---------------- */

    private void confirmMain() {
        if (!ready) { toast("Сначала активируй Shizuku"); return; }
        new AlertDialog.Builder(this)
                .setTitle("Отключить мусор?")
                .setMessage("Будут отключены: Glance/карусель, лента и ассистент, браузер, маркет, темы, облако, музыка, Hot Apps, Game Space, телеметрия, сервисы Facebook/Netflix.\n\nВсё можно вернуть кнопкой «Вернуть всё как было».")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Поехали", (d, w) -> exec(disableScript(SAFE), "Отключение мусора"))
                .show();
    }

    private void confirmExtra() {
        if (!ready) { toast("Сначала активируй Shizuku"); return; }
        new AlertDialog.Builder(this)
                .setTitle("Лента Google — осторожно")
                .setMessage("Вместе с лентой слева отключатся поиск Google и Ассистент, а также экран рекомендаций при установке APK. Если пользуешься поиском Google — не включай это.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Всё равно отключить", (d, w) -> exec(disableScript(EXTRA), "Лента Google"))
                .show();
    }

    /* ---------------- скрипты ---------------- */

    private String disableScript(String pkgs) {
        return "for p in " + pkgs + "; do\n" +
               "  if pm path \"$p\" >/dev/null 2>&1; then\n" +
               "    r=$(pm disable-user --user 0 \"$p\" 2>&1)\n" +
               "    case \"$r\" in\n" +
               "      *disabled-user*) echo \"OFF $p\" ;;\n" +
               "      *) echo \"ERR $p :: $r\" ;;\n" +
               "    esac\n" +
               "  else echo \"SKIP $p\"; fi\n" +
               "done";
    }

    private String restoreScript(String pkgs) {
        return "for p in " + pkgs + "; do\n" +
               "  if pm path \"$p\" >/dev/null 2>&1; then\n" +
               "    r=$(pm enable \"$p\" 2>&1)\n" +
               "    case \"$r\" in\n" +
               "      *enabled*|*default*) echo \"ON $p\" ;;\n" +
               "      *) echo \"ERR $p :: $r\" ;;\n" +
               "    esac\n" +
               "  else echo \"SKIP $p\"; fi\n" +
               "done";
    }

    private String scanScript(String pkgs) {
        return "D=$(pm list packages -d)\n" +
               "for p in " + pkgs + "; do\n" +
               "  if pm path \"$p\" >/dev/null 2>&1; then\n" +
               "    if echo \"$D\" | grep -q \"package:$p\"; then echo \"OFF $p (уже отключен)\";\n" +
               "    else echo \"ON $p (включен)\"; fi\n" +
               "  else echo \"SKIP $p (нет на телефоне)\"; fi\n" +
               "done";
    }

    private void exec(String script, String title) {
        if (!ready) { toast("Сначала активируй Shizuku"); return; }
        log("\n▶ " + title);
        setBusy(true);
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            int off = 0, on = 0, skip = 0, err = 0;
            try {
                Process p = Shizuku.newProcess(new String[]{"sh", "-c", script}, null, null);
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("OFF ")) off++;
                    else if (line.startsWith("ON ")) on++;
                    else if (line.startsWith("SKIP ")) skip++;
                    else if (line.startsWith("ERR ")) err++;
                    sb.append(line).append('\n');
                }
                p.waitFor();
                final String res = sb.toString();
                final String sum = "отключено: " + off + " · включено: " + on +
                                   " · нет: " + skip + " · ошибок: " + err;
                runOnUiThread(() -> { log(res + "✔ " + sum); setBusy(false); toast(sum); });
            } catch (Throwable t) {
                final String e = String.valueOf(t);
                runOnUiThread(() -> { log("Ошибка: " + e + "\nОбнови Shizuku и запусти её заново."); setBusy(false); });
            }
        }).start();
    }

    /* ---------------- утилиты ---------------- */

    private void log(String s) {
        logView.append(s + "\n");
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void intent(String action) {
        try { startActivity(new Intent(action)); }
        catch (Throwable t) { toast("Не удалось открыть настройки"); }
    }

    private void launch(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) throw new Exception();
            startActivity(i);
        } catch (Throwable t) { toast("Приложение не установлено"); }
    }

    private void open(String uri, String web) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri))); }
        catch (Throwable t) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(web))); }
            catch (Throwable ignored) { toast("Открой магазин приложений вручную"); }
        }
    }

    private void appInfo(String pkg) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + pkg));
            startActivity(i);
        } catch (Throwable t) { toast("Приложение не найдено"); }
    }
}
