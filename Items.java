public class Items { // Itemsという名前のクラス（プログラムのまとまり）を作ります
    public static void main(String[] args) { // プログラムの実行がここから始まります
        String[] todos = { "牛乳を買う", "卵を買う", "パンを買う", "掃除をする" }; // Todoの文字列3件を配列（複数の値を入れる箱）に入れます
        boolean[] done = { true, false, false, false }; // 各Todoが済んだかを表す配列（trueは済み、falseは未済み）です
        for (int i = 0; i < todos.length; i++) { // iを使って、Todoを最初から順に取り出します
            String mark = done[i] ? "[済] " : ""; // 済んでいる場合だけ付ける印を決めます
            System.out.println("<li>" + mark + todos[i] + "</li>"); // Todoを<li>と</li>で囲んで1行に出力します
        } // for文（繰り返し）の範囲はここまでです
    } // mainメソッド（実行開始地点）の範囲はここまでです
} // Itemsクラスの範囲はここまでです
