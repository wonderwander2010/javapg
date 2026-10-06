import com.sun.net.httpserver.HttpServer; // ★ SQLite版でも同じHTTPサーバーを使います。
import java.net.InetSocketAddress; // ★ サーバーのポート指定に使います。
import java.nio.charset.StandardCharsets; // ★ HTTPの文字をUTF-8で扱います。
import java.net.URLDecoder; // ★ フォーム文字列を元に戻します。
import java.sql.Connection; // ★ SQLiteとの接続を表します。
import java.sql.DriverManager; // ★ SQLiteへ接続します。
import java.sql.PreparedStatement; // ★ 安全にSQLを実行します。
import java.sql.ResultSet; // ★ SELECT結果を読み取ります。
import java.sql.SQLException; // ★ データベース処理のエラーを扱います。
import java.util.ArrayList; // ★ 一覧結果を一時的に保持します。
import java.util.List; // ★ Todo一覧の型に使います。

class Todo { // ★ Todoの情報をまとめます。
    private final int id; // ★ Todoの番号です。
    private final String title; // ★ Todoの内容です。
    private boolean done; // ★ 完了状態です。

    Todo(int id, String title, boolean done) { // ★ DBから取得した値でTodoを作ります。
        this.id = id; // ★ 番号を保存します。
        this.title = title; // ★ 内容を保存します。
        this.done = done; // ★ 完了状態を保存します。
    }

    int getId() { // ★ Todoの番号を返します。
        return id; // ★ 番号を返します。
    }

    String getTitle() { // ★ Todoの内容を返します。
        return title; // ★ 内容を返します。
    }

    boolean isDone() { // ★ 完了状態を返します。
        return done; // ★ 完了状態を返します。
    }

    void setDone(boolean done) { // ★ 完了状態を変更します。
        this.done = done; // ★ 新しい完了状態を保存します。
    }
}

public class App { // ★ アプリ本体です。
    private static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ 指定されたSQLiteファイルへ接続します。

    private static Connection connect() throws SQLException { // ★ DB接続を作るメソッドです。
        return DriverManager.getConnection(DB_URL); // ★ SQLiteへ接続します。
    }

    private static void initializeDatabase() throws SQLException { // ★ 起動時に表を用意します。
        String sql = "CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"; // ★ 指定の列を持つ表を作ります。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★ 接続とSQL文を準備します。
            statement.executeUpdate(); // ★ 表が無い場合だけ作成します。
        } // ★ 接続とSQL文を閉じます。
    }

    private static List<Todo> loadTodos() throws SQLException { // ★ SELECTで全Todoを読み込みます。
        List<Todo> todos = new ArrayList<>(); // ★ 読み込んだTodoを入れる一覧です。
        String sql = "SELECT id, title, done FROM todos ORDER BY id"; // ★ 一覧取得SQLです。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) { // ★ SELECTを実行して結果を受け取ります。
            while (result.next()) { // ★ 結果がある間、1件ずつ読みます。
                todos.add(new Todo(result.getInt("id"), result.getString("title"), result.getInt("done") != 0)); // ★ DBの値からTodoを作ります。
            } // ★ 読み込みを終えます。
        } // ★ 結果と接続を閉じます。
        return todos; // ★ 一覧を呼び出し元へ返します。
    }

    public static void main(String[] args) throws Exception { // ★ アプリを起動します。
        initializeDatabase(); // ★ 起動時にtodos表を用意します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // ★ 8080番でHTTPサーバーを作ります。
        server.createContext("/", exchange -> { // ★ URLごとの処理を登録します。
            String path = exchange.getRequestURI().getPath(); // ★ アクセスされたパスを取得します。
            String method = exchange.getRequestMethod(); // ★ GETかPOSTか取得します。
            String message; // ★ ブラウザーへ返す内容です。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // ★ 応答の文字コードを指定します。
            if (path.equals("/add") && method.equals("POST")) { // ★ 追加フォームを処理します。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // ★ フォームの送信内容を読みます。
                String value = body.substring(5); // ★ todo= の後ろを取り出します。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★ フォーム文字を元に戻します。
                if (!title.isEmpty()) { // ★ 空の内容でないか確認します。
                    String sql = "INSERT INTO todos (title, done) VALUES (?, 0)"; // ★ Todo追加SQLです。
                    try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★ 接続とSQL文を準備します。
                        statement.setString(1, title); // ★ 内容をSQLの?へ安全に設定します。
                        statement.executeUpdate(); // ★ Todoをデータベースへ追加します。
                    } // ★ 接続とSQL文を閉じます。
                } // ★ 空欄確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★ 一覧へ戻します。
                exchange.sendResponseHeaders(303, -1); // ★ ページ移動を指示します。
                exchange.close(); // ★ 通信を閉じます。
                return; // ★ この要求の処理を終えます。
            } else if (path.equals("/done") && method.equals("GET")) { // ★ 完了リンクを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★ URLの番号部分を取得します。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★ 番号があるか確認します。
                    try { // ★ 番号を数値に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★ URLからTodo番号を取り出します。
                        String sql = "UPDATE todos SET done = 1 WHERE id = ?"; // ★ 指定Todoを完了にするSQLです。
                        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★ 接続とSQL文を準備します。
                            statement.setInt(1, id); // ★ 番号をSQLの?へ安全に設定します。
                            statement.executeUpdate(); // ★ 完了状態を更新します。
                        } // ★ 接続とSQL文を閉じます。
                    } catch (NumberFormatException e) { // ★ 数字でない番号は無視します。
                    } // ★ 番号変換の処理を終えます。
                } // ★ 番号の確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★ 一覧へ戻します。
                exchange.sendResponseHeaders(303, -1); // ★ ページ移動を指示します。
                exchange.close(); // ★ 通信を閉じます。
                return; // ★ この要求の処理を終えます。
            } else if (path.equals("/delete") && method.equals("GET")) { // ★ 削除リンクを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★ URLの番号部分を取得します。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★ 番号があるか確認します。
                    try { // ★ 番号を数値に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★ URLからTodo番号を取り出します。
                        String sql = "DELETE FROM todos WHERE id = ?"; // ★ 指定Todoを削除するSQLです。
                        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★ 接続とSQL文を準備します。
                            statement.setInt(1, id); // ★ 番号をSQLの?へ安全に設定します。
                            statement.executeUpdate(); // ★ Todoをデータベースから削除します。
                        } // ★ 接続とSQL文を閉じます。
                    } catch (NumberFormatException e) { // ★ 数字でない番号は無視します。
                    } // ★ 番号変換の処理を終えます。
                } // ★ 番号の確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★ 一覧へ戻します。
                exchange.sendResponseHeaders(303, -1); // ★ ページ移動を指示します。
                exchange.close(); // ★ 通信を閉じます。
                return; // ★ この要求の処理を終えます。
            } else if (path.equals("/")) { // ★ Todo一覧ページを表示します。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // ★ HTMLとUTF-8を指定します。
                StringBuilder html = new StringBuilder(); // ★ HTML文字列を組み立てます。
                html.append("<!doctype html><html lang='ja'><head><meta charset='UTF-8'>") // ★ HTMLページを開始します。
                    .append("<title>わたしのTodo</title>") // ★ ページタイトルです。
                    .append("<style>body{max-width:40rem;margin:2rem auto;padding:0 1rem;font-size:1rem;}h1{font-size:1.5rem;}input,button{font-size:1rem;}</style>") // ★ 最小限の見た目を整えます。
                    .append("</head><body><h1>わたしのTodo</h1>") // ★ 見出しを表示します。
                    .append("<form method='post' action='/add'><input name='todo'><button>追加</button></form>"); // ★ Todo追加フォームです。
                List<Todo> todos = loadTodos(); // ★ SELECTでDBから最新の一覧を読み込みます。
                if (todos.isEmpty()) { // ★ Todoが0件か確認します。
                    html.append("<p>やることは、いまゼロです</p>"); // ★ 0件のときの案内を表示します。
                } else { // ★ Todoがある場合です。
                    html.append("<ul>"); // ★ 一覧を開始します。
                    for (Todo todo : todos) { // ★ Todoを1件ずつ表示します。
                        String mark = ""; // ★ 未完了なら印を空にします。
                        if (todo.isDone()) { // ★ 完了状態を確認します。
                            mark = " ✔"; // ★ 完了済みに印を付けます。
                        } // ★ 完了状態の確認を終えます。
                        html.append("<li>").append(todo.getTitle()).append(mark).append(" <a href='/done?id=") // ★ 内容と完了リンクを表示します。
                            .append(todo.getId()).append("'>完了</a> <a href='/delete?id=") // ★ 番号と削除リンクを表示します。
                            .append(todo.getId()).append("'>削除</a></li>"); // ★ 一覧行を閉じます。
                    } // ★ 一覧表示を終えます。
                    html.append("</ul>"); // ★ 一覧を閉じます。
                } // ★ 件数ごとの表示を終えます。
                html.append("</body></html>"); // ★ HTMLページを閉じます。
                message = html.toString(); // ★ 完成したページを応答内容にします。
            } else { // ★ それ以外のURLの場合です。
                message = "ページが見つかりません"; // ★ 見つからない旨を返します。
            } // ★ URLごとの処理を終えます。
            byte[] responseBody = message.getBytes(StandardCharsets.UTF_8); // ★ 応答をUTF-8のバイト列にします。
            exchange.sendResponseHeaders(200, responseBody.length); // ★ 成功応答と長さを送ります。
            exchange.getResponseBody().write(responseBody); // ★ ページ内容を送ります。
            exchange.getResponseBody().close(); // ★ 応答を閉じます。
        }); // ★ URL処理の登録を終えます。
        server.start(); // ★ サーバーを起動します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // ★ 起動先を表示します。
    } // ★ mainを終えます。
} // ★ Appクラスを終えます。
