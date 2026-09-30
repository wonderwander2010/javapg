
// Webサーバーを使うための読み込みです。
import com.sun.net.httpserver.HttpServer;
// ポート番号を指定するための読み込みです。
import java.net.InetSocketAddress;
// URL内の変換された文字を元に戻すための読み込みです。
import java.net.URLDecoder;
// Todoを並べて入れるリストを使うための読み込みです。
import java.util.List;
// Todoを入れるリストを作るための読み込みです。
import java.util.ArrayList;

// Appという名前のプログラムです。
public class App {
    // プログラムの開始地点です。
    public static void main(String[] args) throws Exception {
        // 8080番ポートで待ち受けるサーバーを用意します。【1】
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // 「/」にアクセスされたときの処理を登録します。【1】
        server.createContext("/", exchange -> {
            // ブラウザに返す文字です。【毎】
            // アクセスされたパスを取り出します。
            String path = exchange.getRequestURI().getPath();
            String message;
            // 返す文字の形式と文字コードの初期値を指定します。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            // パスが /hello かどうかを比べます。
            if (path.equals("/hello")) {
                // URLの ? の後ろにあるクエリを取り出します。
                String query = exchange.getRequestURI().getRawQuery();
                // クエリがない場合は、名前を「ゲスト」にします。
                String name = query == null ? "ゲスト" : URLDecoder.decode(query.substring(5), "UTF-8");
                // 名前を挨拶の文字列につなげます。
                message = "こんにちは、" + name + "さん！";
                // パスが /bye かどうかを比べます。
            } else if (path.equals("/todos")) {
                // Todoの文字列を入れるリストを作ります。
                List<String> todos = new ArrayList<>();
                // 買い物のTodoを1件追加します。
                todos.add("牛乳を買う");
                // 買い物のTodoを1件追加します。
                todos.add("卵を買う");
                // 買い物のTodoを1件追加します。
                todos.add("パンを買う");
                todos.add("掃除をする");
                // HTMLのリストを開きます。
                String html = "<ul>";
                // Todoを1件ずつ取り出します。
                for (String todo : todos) {
                    // TodoをHTMLの項目として加えます。
                    html += "<li>" + todo + "</li>";
                }
                // HTMLのリストを閉じます。
                html += "</ul>";
                // 作ったHTMLを応答の中身にします。
                message = html;
                // このパスの応答形式をHTMLにします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else if (path.equals("/bye")) {
                message = "さようなら！";
            } else if (path.equals("/menu")) {
                message = "今夜はカレー";
            } else {
                message = "ページが見つかりません";
            }
            // 返す文字をUTF-8の送信用データにします。【毎】
            byte[] body = message.getBytes("UTF-8");
            // 成功を示す番号と、返すデータの長さを送ります。【毎】
            exchange.sendResponseHeaders(200, body.length);
            // データをブラウザへ書き込みます。【毎】
            exchange.getResponseBody().write(body);
            // 送信を終えます。【毎】
            exchange.getResponseBody().close();
        });
        // サーバーの待ち受けを開始します。【1】
        server.start();
        // 起動したことと、停止方法を表示します。【1】
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }
}
