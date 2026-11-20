package ru.altai.antimusor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

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
    private LinearLayout guideBox, yandexRows, catalogRows;
    private Button permBtn, bigBtn, scanBtn, restoreBtn, extraBtn, checkBtn,
            yandexBtn, launcherBtn, catMarkBtn, catHideBtn, catRemoveBtn;
    private boolean ready = false, busy = false, binding = false;

    private final ArrayList<String> yandexAll = new ArrayList<>();
    private final ArrayList<String> yandexPkgs = new ArrayList<>();
    private final ArrayList<CheckBox> yandexChecks = new ArrayList<>();

    private final ArrayList<String> catAll = new ArrayList<>();
    private final ArrayList<String> catPkgs = new ArrayList<>();
    private final ArrayList<CheckBox> catChecks = new ArrayList<>();
    private final ArrayList<Integer> catCats = new ArrayList<>();

    private String launcherPkg = null;
    private IBinder shellBinder;
    private Shizuku.UserServiceArgs svcArgs;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder b) {
            shellBinder = b;
            binding = false;
            runOnUiThread(() -> { refresh(); log("✅ Shell-служба готова, можно работать."); });
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            shellBinder = null;
            binding = false;
            runOnUiThread(() -> refresh());
        }
    };

    private final Shizuku.OnBinderReceivedListener received = () -> runOnUiThread(this::refresh);
    private final Shizuku.OnBinderDeadListener dead = () -> runOnUiThread(() -> {
        shellBinder = null;
        binding = false;
        refresh();
    });
    private final Shizuku.OnRequestPermissionResultListener permResult =
            (rq, gr) -> runOnUiThread(this::refresh);

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF0B1020);
        buildUi();
        detectLauncher();
        fillYandex();
        fillCatalog();
        Shizuku.addBinderReceivedListenerSticky(received);
        Shizuku.addBinderDeadListener(dead);
        Shizuku.addRequestPermissionResultListener(permResult);
        refresh();
        log("Готов к работе. Активируй Shizuku и жми большую кнопку.");
    }

    @Override protected void onDestroy() {
        try { if (svcArgs != null) Shizuku.unbindUserService(svcArgs, conn, true); } catch (Throwable ignored) {}
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
        scanBtn.setOnClickListener(v -> exec(scanScript(allTargets()), "Проверка списка"));
        root.addView(scanBtn);

        restoreBtn = button("Вернуть всё как было", false);
        restoreBtn.setOnClickListener(v -> exec(restoreScript(allTargets()), "Восстановление"));
        root.addView(restoreBtn);

        extraBtn = button("Дополнительно: лента Google (осторожно)", false);
        extraBtn.setOnClickListener(v -> confirmExtra());
        root.addView(extraBtn);

        LinearLayout yx = card();
        yx.addView(text("Лента Яндекса (свайп влево)", 16, 0xFFEEF2FF, true));
        yx.addView(text("Найденные приложения Яндекса. Галочки уже стоят на поставщиках ленты. Отключи выбранные и перезапусти рабочий стол — свайп-страница исчезнет.", 13, 0xFFA9B4DC, false));
        yandexRows = new LinearLayout(this);
        yandexRows.setOrientation(LinearLayout.VERTICAL);
        yx.addView(yandexRows);
        yandexBtn = button("Отключить выбранное", false);
        yandexBtn.setOnClickListener(v -> confirmYandex());
        launcherBtn = button("Перезапустить рабочий стол", false);
        launcherBtn.setOnClickListener(v -> restartLauncher());
        yx.addView(yandexBtn);
        yx.addView(launcherBtn);
        yx.addView(text("Если Яндекс-приложений не нашлось: щипок по рабочему столу → Настройки → выключи «Лента новостей» / «Левый экран» (название может отличаться).", 12, 0xFF7C88B4, false));
        root.addView(yx);

        LinearLayout ct = card();
        ct.addView(text("Предустановленные приложения", 16, 0xFFEEF2FF, true));
        ct.addView(text("VK, Одноклассники, Mail.ru, Маруся, Rutube, МойОфис и другие. «Скрыть» — отключить (иконка исчезнет, данные останутся). «Удалить для меня» — снести для текущего пользователя, данные сохранятся. Возврат — «Вернуть всё как было».", 13, 0xFFA9B4DC, false));
        catalogRows = new LinearLayout(this);
        catalogRows.setOrientation(LinearLayout.VERTICAL);
        ct.addView(catalogRows);
        catMarkBtn = button("Отметить рекомендованные", false);
        catMarkBtn.setOnClickListener(v -> markRecommended());
        catHideBtn = button("Скрыть выбранные (отключить)", false);
        catHideBtn.setOnClickListener(v -> confirmCat(false));
        catRemoveBtn = button("Удалить выбранные для меня", false);
        catRemoveBtn.setOnClickListener(v -> confirmCat(true));
        ct.addView(catMarkBtn);
        ct.addView(catHideBtn);
        ct.addView(catRemoveBtn);
        ct.addView(text("Виджет «Выбрать контакт» и ярлык «Ускорение системы»: долгий тап → «Убрать / Удалить с рабочего стола». Mir Pay, Kaspersky и «Честный знак» лучше оставить — они полезные.", 12, 0xFF7C88B4, false));
        root.addView(ct);

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

        root.addView(text("Отключается и удаляется только для текущего пользователя. Системный раздел не трогается, обновления и гарантия целы.", 12, 0xFF7C88B4, false));
        setContentView(sv);
    }

    /* ---------------- каталоги приложений ---------------- */

    private void detectLauncher() {
        try {
            Intent h = new Intent(Intent.ACTION_MAIN);
            h.addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo ri = getPackageManager().resolveActivity(h, 0);
            if (ri != null && ri.activityInfo != null) launcherPkg = ri.activityInfo.packageName;
        } catch (Throwable ignored) {}
    }

    private void fillYandex() {
        yandexRows.removeAllViews();
        yandexAll.clear(); yandexPkgs.clear(); yandexChecks.clear();
        try {
            PackageManager pm = getPackageManager();
            List<PackageInfo> list = pm.getInstalledPackages(0);
            for (PackageInfo pi : list) {
                ApplicationInfo ai = pi.applicationInfo;
                if (ai == null) continue;
                String pn = ai.packageName;
                String label = pm.getApplicationLabel(ai).toString();
                String low = label.toLowerCase();
                boolean rel = pn.contains("yandex") || low.contains("яндекс")
                        || low.contains("дзен") || low.contains("yandex");
                if (!rel) continue;
                yandexAll.add(pn);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(6), 0, dp(6));
                CheckBox cb = new CheckBox(this);
                cb.setChecked(pn.contains("searchplugin") || pn.equals("ru.yandex.zen") || low.contains("дзен"));
                LinearLayout tx = new LinearLayout(this);
                tx.setOrientation(LinearLayout.VERTICAL);
                tx.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tx.addView(text(label, 14, 0xFFDDE4FF, true));
                TextView t2 = text(pn, 11, 0xFF8FA0CC, false);
                t2.setTypeface(Typeface.MONOSPACE);
                tx.addView(t2);
                row.addView(cb);
                row.addView(tx);
                yandexRows.addView(row);
                yandexChecks.add(cb);
                yandexPkgs.add(pn);
            }
        } catch (Throwable t) {
            log("Не удалось прочитать список приложений: " + t);
        }
        if (yandexAll.isEmpty()) {
            yandexRows.addView(text("Яндекс-приложения не найдены.", 13, 0xFFA9B4DC, false));
        }
    }

    private void fillCatalog() {
        catalogRows.removeAllViews();
        catAll.clear(); catPkgs.clear(); catChecks.clear(); catCats.clear();
        ArrayList<Object[]> found = new ArrayList<>();
        try {
            PackageManager pm = getPackageManager();
            for (PackageInfo pi : pm.getInstalledPackages(0)) {
                ApplicationInfo ai = pi.applicationInfo;
                if (ai == null) continue;
                String pn = ai.packageName;
                if (yandexAll.contains(pn)) continue;
                String label = pm.getApplicationLabel(ai).toString();
                String low = label.toLowerCase();
                boolean rel = pn.startsWith("ru.yandex") || pn.startsWith("com.yandex")
                        || pn.startsWith("com.vk") || pn.startsWith("com.vkontakte")
                        || pn.startsWith("ru.ok") || pn.startsWith("ru.mail")
                        || pn.startsWith("ru.nspk") || pn.startsWith("com.kms")
                        || pn.startsWith("ru.rutube") || pn.startsWith("ru.crpt")
                        || pn.startsWith("ru.myoffice")
                        || low.contains("яндекс") || low.contains("дзен") || low.contains("маруся")
                        || low.contains("вконтакте") || low.contains("vk") || low.contains("одноклассники")
                        || low.contains("mail.ru") || low.contains("почта") || low.contains("mir pay")
                        || low.contains("мойофис") || low.contains("честный") || low.contains("kaspersky")
                        || low.contains("касперский") || low.contains("rutube") || low.contains("рутуб")
                        || low.contains("ускорение");
                if (!rel) continue;
                int cat;
                if (pn.contains("nspk") || low.contains("mir pay") || low.contains("kaspersky")
                        || low.contains("касперский") || low.contains("ускорение")) cat = 2;
                else if (low.contains("дзен") || low.contains("маруся") || low.contains("rutube")
                        || low.contains("рутуб") || pn.contains("rutube") || pn.contains("marusia")) cat = 0;
                else cat = 1;
                found.add(new Object[]{pn, label, cat});
            }
        } catch (Throwable t) {
            log("Каталог: " + t);
        }
        found.sort((a, b) -> {
            int c = Integer.compare((Integer) a[2], (Integer) b[2]);
            return c != 0 ? c : ((String) a[1]).compareToIgnoreCase((String) b[1]);
        });
        for (Object[] o : found) {
            String pn = (String) o[0];
            String label = (String) o[1];
            int cat = (Integer) o[2];
            catAll.add(pn);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            CheckBox cb = new CheckBox(this);
            cb.setChecked(cat == 0);
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tx.addView(text(label, 14, 0xFFDDE4FF, true));
            TextView t2 = text(pn, 11, 0xFF8FA0CC, false);
            t2.setTypeface(Typeface.MONOSPACE);
            tx.addView(t2);
            TextView chip = text(cat == 0 ? "смело убирай" : cat == 1 ? "если не пользуешься" : "лучше оставить",
                    10, cat == 0 ? 0xFF3CE8B0 : cat == 1 ? 0xFFFFB648 : 0xFF93A0CC, true);
            chip.setBackground(round(cat == 0 ? 0x1F3CE8B0 : cat == 1 ? 0x1FFFB648 : 0x1F93A0CC, 8, 0));
            chip.setPadding(dp(7), dp(3), dp(7), dp(3));
            row.addView(cb);
            row.addView(tx);
            row.addView(chip);
            catalogRows.addView(row);
            catChecks.add(cb);
            catPkgs.add(pn);
            catCats.add(cat);
        }
        if (found.isEmpty()) {
            catalogRows.addView(text("Предустановленные приложения из каталога не найдены.", 13, 0xFFA9B4DC, false));
        }
    }

    private String join(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String p : list) sb.append(p).append(' ');
        return sb.toString().trim();
    }

    private String allTargets() {
        return (SAFE + " " + EXTRA + " " + join(yandexAll) + " " + join(catAll)).trim();
    }

    private String selected(ArrayList<CheckBox> checks, ArrayList<String> pkgs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < checks.size(); i++) {
            if (checks.get(i).isChecked()) sb.append(pkgs.get(i)).append(' ');
        }
        return sb.toString().trim();
    }

    private void markRecommended() {
        int n = 0;
        for (int i = 0; i < catChecks.size(); i++) {
            if (catCats.get(i) <= 1) { catChecks.get(i).setChecked(true); n++; }
        }
        toast("Отмечено: " + n + ". Полезные приложения не тронуты.");
        sUiLog();
    }

    private void sUiLog() { log("Отмечены рекомендованные к уборке приложения."); }

    private void confirmYandex() {
        if (!ready) { toast("Сначала активируй Shizuku"); return; }
        String pkgs = selected(yandexChecks, yandexPkgs);
        if (pkgs.isEmpty()) { toast("Отметь хотя бы одно приложение"); return; }
        new AlertDialog.Builder(this)
                .setTitle("Отключить ленту Яндекса?")
                .setMessage("Лента при свайпе исчезнет. Возврат — кнопкой «Вернуть всё как было».")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Отключить", (d, w) -> exec(disableScript(pkgs), "Лента Яндекса"))
                .show();
    }

    private void confirmCat(boolean uninstall) {
        if (!ready) { toast("Сначала активируй Shizuku"); return; }
        String pkgs = selected(catChecks, catPkgs);
        if (pkgs.isEmpty()) { toast("Отметь хотя бы одно приложение"); return; }
        int n = pkgs.split(" ").length;
        String msg = uninstall
                ? "Будет удалено для текущего пользователя: " + n + " прил. Данные сохранятся, системный раздел не тронется. Возврат — «Вернуть всё как было»."
                : "Будет скрыто и остановлено: " + n + " прил. Иконки исчезнут, данные останутся. Возврат — «Вернуть всё как было».";
        new AlertDialog.Builder(this)
                .setTitle(uninstall ? "Удалить выбранные для меня?" : "Скрыть выбранные?")
                .setMessage(msg)
                .setNegativeButton("Отмена", null)
                .setPositiveButton(uninstall ? "Удалить" : "Скрыть",
                        (d, w) -> exec(uninstall ? uninstallScript(pkgs) : disableScript(pkgs),
                                uninstall ? "Удаление для пользователя" : "Скрытие приложений"))
                .show();
    }

    private void restartLauncher() {
        if (!ready) { toast("Сначала активируй Shizuku"); return; }
        if (launcherPkg == null) { toast("Лаунчер не определён"); return; }
        exec("am force-stop " + launcherPkg + "\necho OK рабочий стол перезапущен", "Перезапуск рабочего стола");
    }

    /* ---------------- состояние ---------------- */

    private void refresh() {
        boolean ping = false, granted = false;
        try { ping = Shizuku.pingBinder(); } catch (Throwable ignored) {}
        if (ping) {
            try { granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
            catch (Throwable ignored) {}
        }
        if (ping && granted && shellBinder == null && !binding) {
            binding = true;
            try {
                svcArgs = new Shizuku.UserServiceArgs(
                        new ComponentName(getPackageName(), ShellService.class.getName()))
                        .daemon(false)
                        .processNameSuffix("shell")
                        .debuggable(false)
                        .version(1);
                Shizuku.bindUserService(svcArgs, conn);
            } catch (Throwable t) {
                binding = false;
                log("Не удалось запустить службу: " + t);
            }
        }
        ready = ping && granted && shellBinder != null;

        if (ready) {
            statusTitle.setText("✅ Всё готово");
            statusTitle.setTextColor(0xFF3CE8B0);
            statusText.setText("Shizuku работает, shell-служба поднята. Жми большую кнопку.");
        } else if (ping && granted) {
            statusTitle.setText("⏳ Запускаю службу…");
            statusTitle.setTextColor(0xFFFFB648);
            statusText.setText("Разрешение есть, поднимаю shell-службу. Обычно пара секунд.");
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
        Button[] all = {bigBtn, scanBtn, restoreBtn, extraBtn, yandexBtn, launcherBtn, catMarkBtn, catHideBtn, catRemoveBtn};
        for (Button x : all) {
            x.setEnabled(ready && !b);
            x.setAlpha(ready && !b ? 1f : 0.4f);
        }
        bigBtn.setText(b ? "Работаю…" : "ОТКЛЮЧИТЬ ВСЁ ОДНОЙ КНОПКОЙ");
    }

    /* ---------------- диалоги ---------------- */

    private void confirmMain() {
        if (!ready) { toast("Сначала активируй Shizuku"); return; }
        new AlertDialog.Builder(this)
                .setTitle("Отключить мусор?")
                .setMessage("Будут отключены: Glance/карусель, лента и ассистент, браузер, маркет, темы, облако, музыка, Hot Apps, Game Space, телеметрия, сервисы Facebook/Netflix.\n\nЛента Яндекса и предустановленные приложения убираются отдельными блоками ниже. Всё можно вернуть кнопкой «Вернуть всё как было».")
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

    private String uninstallScript(String pkgs) {
        return "for p in " + pkgs + "; do\n" +
               "  if pm path \"$p\" >/dev/null 2>&1; then\n" +
               "    r=$(pm uninstall -k --user 0 \"$p\" 2>&1)\n" +
               "    case \"$r\" in\n" +
               "      *Success*) echo \"OFF $p (удалено для меня)\" ;;\n" +
               "      *) echo \"ERR $p :: $r\" ;;\n" +
               "    esac\n" +
               "  else echo \"SKIP $p\"; fi\n" +
               "done";
    }

    private String restoreScript(String pkgs) {
        return "for p in " + pkgs + "; do\n" +
               "  pm install-existing \"$p\" >/dev/null 2>&1\n" +
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
        if (!ready || shellBinder == null) { toast("Сначала активируй Shizuku"); return; }
        log("\n▶ " + title);
        setBusy(true);
        final IBinder b = shellBinder;
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            int off = 0, on = 0, skip = 0, err = 0;
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            try {
                data.writeString(script);
                b.transact(1, data, reply, 0);
                String out = reply.readString();
                if (out == null) out = "ERR пустой ответ службы\n";
                for (String line : out.split("\n")) {
                    if (line.startsWith("OFF ")) off++;
                    else if (line.startsWith("ON ")) on++;
                    else if (line.startsWith("SKIP ")) skip++;
                    else if (line.startsWith("ERR ")) err++;
                    sb.append(line).append('\n');
                }
                final String res = sb.toString();
                final String sum = "отключено: " + off + " · включено: " + on +
                                   " · нет: " + skip + " · ошибок: " + err;
                runOnUiThread(() -> {
                    log(res + "✔ " + sum);
                    setBusy(false);
                    fillYandex();
                    fillCatalog();
                    toast(sum);
                });
            } catch (Throwable t) {
                final String e = String.valueOf(t);
                runOnUiThread(() -> {
                    log("Ошибка: " + e + "\nЗапусти Shizuku заново и повтори.");
                    shellBinder = null;
                    binding = false;
                    setBusy(false);
                    refresh();
                });
            } finally {
                data.recycle();
                reply.recycle();
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
