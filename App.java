
// Webサーバーを使うための読み込みです。
import com.sun.net.httpserver.HttpServer;
// ポート番号を指定するための読み込みです。
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets; // UTF-8の文字コードを使います。
// URL内の変換された文字を元に戻すための読み込みです。
import java.net.URLDecoder;
// Todoを並べて入れるリストを使うための読み込みです。
import java.util.List;
// Todoを入れるリストを作るための読み込みです。
import java.util.ArrayList;

class Todo { // ★変更 Todoの情報をまとめるクラスです。
    private final int id; // ★変更 Todoの番号です。
    private final String title; // ★変更 Todoの内容です。
    private boolean done; // ★変更 完了したかを記録します。

    Todo(int id, String title) { // ★変更 新しいTodoを作ります。
        this.id = id;
        this.title = title;
        this.done = false;
    }

    int getId() { // ★変更 Todoの番号を読み出します。
        return id;
    }

    String getTitle() { // ★変更 Todoの内容を読み出します。
        return title;
    }

    boolean isDone() { // ★変更 完了したかを読み出します。
        return done;
    }

    void setDone(boolean done) { // ★変更 完了したかを書き換えます。
        this.done = done;
    }
}

// Appという名前のプログラムです。
public class App {
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを保存するリストです。
    static int nextId = 1; // ★変更 次に使うTodoの番号です。

    // プログラムの開始地点です。
    public static void main(String[] args) throws Exception {
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更 起動時のサンプルTodoを追加します。
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更 起動時のサンプルTodoを作ります。
        egg.setDone(true); // ★変更 卵のTodoを完了済みにします。
        todos.add(egg); // ★変更 卵のTodoをリストに追加します。
        // 8080番ポートで待ち受けるサーバーを用意します。【1】
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // 「/」にアクセスされたときの処理を登録します。【1】
        server.createContext("/", exchange -> {
            // ブラウザに返す文字です。【毎】
            // アクセスされたパスを取り出します。
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod(); // GETかPOSTかを取り出します。
            String message;
            // 返す文字の形式と文字コードの初期値を指定します。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            // パスが /hello かどうかを比べます。
            if (path.equals("/add") && method.equals("POST")) { // 追加フォームから届いたTodoを処理します。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送信された内容を読み取ります。
                String value = body.substring(5); // 「todo=」を取り除きます。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更 フォームの文字をTodoの内容として受け取ります。
                if (!title.isEmpty()) { // 空でないときだけ追加します。
                    todos.add(new Todo(nextId, title)); // ★変更 新しい番号でTodoを追加します。
                    nextId++; // ★変更 次に使う番号を進めます。
                }
                exchange.getResponseHeaders().set("Location", "/"); // 一覧のページへ戻します。
                exchange.sendResponseHeaders(303, -1); // 追加後にページを移動します。
                exchange.close(); // 通信を閉じます。
                return; // この分岐の処理を終えます。
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加 完了リンクのアクセスを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLから番号を受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 番号があるか確認します。
                    try { // ★追加 番号を数に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 番号を数にします。
                        for (Todo todo : todos) { // ★追加 Todoを1件ずつ確認します。
                            if (todo.getId() == id) { // ★追加 番号が一致するか調べます。
                                todo.setDone(true); // ★追加 Todoを完了にします。
                                break; // ★追加 一致したので繰り返しを終えます。
                            }
                        }
                    } catch (NumberFormatException e) { // ★追加 数字でない番号は何もせず受け止めます。
                    }
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 一覧のページへ戻します。
                exchange.sendResponseHeaders(303, -1); // ★追加 ページを移動します。
                exchange.close(); // ★追加 通信を閉じます。
                return; // ★追加 この分岐を抜けます。
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加 削除リンクのアクセスを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLから番号を受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 番号があるか確認します。
                    try { // ★追加 番号を数に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 番号を数にします。
                        todos.removeIf(todo -> todo.getId() == id); // ★追加 番号が一致するTodoを削除します。
                    } catch (NumberFormatException e) { // ★追加 数字でない番号は何もせず受け止めます。
                    }
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 一覧のページへ戻します。
                exchange.sendResponseHeaders(303, -1); // ★追加 ページを移動します。
                exchange.close(); // ★追加 通信を閉じます。
                return; // ★追加 この分岐を抜けます。
            } else if (path.equals("/")) { // トップページにフォームとTodo一覧を表示します。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // HTML形式で返します。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // Todo追加フォームと一覧を始めます。
                for (Todo todo : todos) { // ★変更 Todoを1件ずつ取り出します。
                    String mark = ""; // ★変更 未完了のときは印を空にします。
                    if (todo.isDone()) { // ★変更 完了済みかを調べます。
                        mark = " ✔"; // ★変更 完了済みの印を付けます。
                    }
                    html += "<li>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId()
                            + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                                                                                                // Todoの番号付き完了・削除リンクを加えます。
                }
                html += "</ul>"; // HTMLの一覧を閉じます。
                message = html; // 作ったHTMLを応答の中身にします。
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
