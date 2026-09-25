public class Item {
    // プログラムの実行がここから始まります。
    public static void main(String[] args) {
        // titleという名前の文字列変数（文字を入れる場所）に「勉強をする」を入れます。
        String title = "勉強をする";
        // 文字列を「+」でつないで、リスト項目のHTML（Webページ用の文字列）を作ります。
        String html = "<li>" + title + "</li>";
        // 作ったHTML文字列をターミナル（文字を表示する画面）に1行出します。
        System.out.println(html);
        boolean done = false;
        System.out.println(done);
        int count = 3;
        System.out.println("いま" + count + "件");
    }
}
