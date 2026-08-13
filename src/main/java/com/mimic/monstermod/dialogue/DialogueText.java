package com.mimic.monstermod.dialogue;

/**
 * 会話テキストの記法まわりのユーティリティ。
 *
 * 色指定は「&」記法で書く(例: &c赤い文字&r通常)。
 * Minecraftの装飾コードは「§」だが、ゲーム内のテキスト欄から「§」を入力するのは
 * 困難なため、入力しやすい「&」で書いてもらい表示直前に変換する。
 * 名前・本文のどちらでも使え、本文の一部だけ色を変えることもできる。
 *
 * 「&&」と書くと素の「&」になる(エスケープ)。
 */
public final class DialogueText {

    private DialogueText() {}

    /** 使える色コード等。ここに無い文字は変換しない(誤変換を防ぐため) */
    private static final String CODES = "0123456789abcdefklmnorABCDEFKLMNOR";

    /** 「&」記法をMinecraftの「§」装飾コードへ変換する */
    public static String colorize(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '&' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == '&') {          // && はそのままの & にする
                    sb.append('&');
                    i++;
                    continue;
                }
                if (CODES.indexOf(next) >= 0) {
                    sb.append('§').append(next);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 装飾コードを除いた「見える文字数」を数える。
     * タイプライター表示で「何文字目まで出すか」を決めるのに使う。
     */
    public static int visibleLength(String colorized) {
        int n = 0;
        for (int i = 0; i < colorized.length(); i++) {
            if (colorized.charAt(i) == '§') { i++; continue; }
            n++;
        }
        return n;
    }

    /**
     * 見える文字を先頭から count 文字だけ残した文字列を返す。
     * 装飾コード(§x)は文字数に数えず、切れ目で壊れないようそのまま維持する。
     */
    public static String takeVisible(String colorized, int count) {
        if (count >= visibleLength(colorized)) return colorized;
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < colorized.length(); i++) {
            char c = colorized.charAt(i);
            if (c == '§' && i + 1 < colorized.length()) {
                sb.append(c).append(colorized.charAt(i + 1)); // コードは常に残す
                i++;
                continue;
            }
            if (shown >= count) break;
            sb.append(c);
            shown++;
        }
        return sb.toString();
    }
}
