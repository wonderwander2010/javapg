import com.sun.net.httpserver.HttpServer; // ★ HTTPサーバーを使います。
import java.net.InetSocketAddress; // ★ サーバーのポートを指定します。
import java.net.URLDecoder; // ★ フォームの文字列を元に戻します。
import java.nio.charset.StandardCharsets; // ★ 文字をUTF-8で扱います。
import java.sql.Connection; // ★ SQLite接続を表します。
import java.sql.DriverManager; // ★ SQLiteへ接続します。
import java.sql.PreparedStatement; // ★ SQLを安全に実行します。
import java.sql.ResultSet; // ★ SELECT結果を読み取ります。
import java.sql.SQLException; // ★ DB操作のエラーを扱います。
import java.util.ArrayList; // ★ Todo一覧を作ります。
import java.util.List; // ★ Todo一覧の型に使います。

class Todo { // ★ Todoの情報を保持します。
    private final int id; // ★ Todoの番号です。
    private final String title; // ★ Todoの内容です。
    private final boolean done; // ★ 完了状態です。

    Todo(int id, String title, boolean done) { // ★ DBからTodoを作ります。
        this.id = id; // ★ 番号を保存します。
        this.title = title; // ★ 内容を保存します。
        this.done = done; // ★ 完了状態を保存します。
    }

    int getId() { // ★ 番号を返します。
        return id; // ★ 番号を返します。
    }

    String getTitle() { // ★ 内容を返します。
        return title; // ★ 内容を返します。
    }

    boolean isDone() { // ★ 完了状態を返します。
        return done; // ★ 完了状態を返します。
    }
}

public class App { // ★ アプリ本体です。
    private static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ 保存先SQLiteファイルです。

    private static Connection connect() throws SQLException { // ★ SQLite接続を作ります。
        return DriverManager.getConnection(DB_URL); // ★ データベースへ接続します。
    }

    private static void initializeDatabase() throws SQLException { // ★ 起動時にテーブルを用意します。
        String sql = "CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"; // ★
                                                                                                            // 指定された構造の表です。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // DB接続とSQL文を用意します。
            statement.executeUpdate(); // ★ 表が無ければ作ります。
        } // ★ 接続とSQL文を閉じます。
    }

    private static List<Todo> loadTodos() throws SQLException { // ★ DBからTodo一覧を読み込みます。
        List<Todo> todos = new ArrayList<>(); // ★ 結果を入れる一覧です。
        String sql = "SELECT id, title, done FROM todos ORDER BY id"; // ★ 全件を番号順に読むSQLです。
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) { // ★ SELECTを実行します。
            while (result.next()) { // ★ 結果を1行ずつ読みます。
                todos.add(new Todo(result.getInt("id"), result.getString("title"), result.getInt("done") != 0)); // ★
                                                                                                                 // 行をTodoにします。
            } // ★ 読み込みを終えます。
        } // ★ 結果と接続を閉じます。
        return todos; // ★ 一覧を返します。
    }

    private static void addTodo(String title) throws SQLException { // ★ Todoをデータベースへ追加します。
        String sql = "INSERT INTO todos (title, done) VALUES (?, 0)"; // ★ 内容を追加するSQLです。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // DB接続とSQL文を用意します。
            statement.setString(1, title); // ★ 内容をSQLの?に設定します。
            statement.executeUpdate(); // ★ Todoを追加します。
        } // ★ 接続とSQL文を閉じます。
    }

    private static void markDone(int id) throws SQLException { // ★ Todoを完了にします。
        String sql = "UPDATE todos SET done = 1 WHERE id = ?"; // ★ 完了状態を更新するSQLです。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // DB接続とSQL文を用意します。
            statement.setInt(1, id); // ★ 対象の番号をSQLの?に設定します。
            statement.executeUpdate(); // ★ Todoを完了状態にします。
        } // ★ 接続とSQL文を閉じます。
    }

    private static void deleteTodo(int id) throws SQLException { // ★ Todoを削除します。
        String sql = "DELETE FROM todos WHERE id = ?"; // ★ 指定番号を削除するSQLです。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // DB接続とSQL文を用意します。
            statement.setInt(1, id); // ★ 対象の番号をSQLの?に設定します。
            statement.executeUpdate(); // ★ Todoを削除します。
        } // ★ 接続とSQL文を閉じます。
    }

    private static int deleteCompletedTodos() throws SQLException { // 完了済みのTodoをまとめて削除します。
        String sql = "DELETE FROM todos WHERE done = 1"; // 完了済みの行だけを削除します。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate(); // 削除できた件数を返します。
        }
    }

    private static String escapeHtml(String value) { // ★ HTMLで特別な意味を持つ文字を安全な表記にします。
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") // ★ 主要な記号を置き換えます。
                .replace("\"", "&quot;").replace("'", "&#39;"); // ★ 引用符も置き換えます。
    }

    private static String renderPage(int deletedCount) throws SQLException { // ★ DBの一覧からHTMLページを作ります。
        List<Todo> todos = loadTodos(); // ★ SELECTで最新の一覧を読み込みます。
        StringBuilder html = new StringBuilder(); // ★ HTMLを組み立てます。
        html.append("<!doctype html><html lang='ja'><head><meta charset='UTF-8'>") // ★ HTMLの先頭です。
                .append("<title>わたしのTodo</title>") // ★ ページタイトルです。
                .append("<style>body{max-width:40rem;margin:2rem auto;padding:0 1rem;font-size:1rem;}h1{font-size:1.5rem;}input,button{font-size:1rem;}#celebration{padding:.8rem 1rem;background:#fff5cc;border-radius:.5rem;animation:fadeout 5s 2s forwards;}#confetti{position:fixed;inset:0;pointer-events:none;overflow:hidden}@keyframes fall{to{transform:translateY(100vh) rotate(720deg);opacity:0}}@keyframes fadeout{to{opacity:0;visibility:hidden}}@media(prefers-reduced-motion:reduce){#celebration{animation:none}#confetti{display:none}}</style>") // ★
                // 控えめな見た目の指定です。
                .append("</head><body><h1>わたしのTodo</h1>") // ★ ページ見出しです。
                .append("<form method='post' action='/add'><input name='todo'><button>追加</button></form>"); // ★
                                                                                                            // 追加フォームです。
        if (deletedCount > 0) {
            html.append("<p id='celebration' role='status'>完了済みのTodoを")
                    .append(deletedCount).append("件片付けました！おつかれさま 🎉</p>")
                    .append("<div id='confetti' aria-hidden='true'>");
            String[] colors = { "#f94144", "#f9c74f", "#43aa8b", "#577590", "#f3722c" };
            for (int i = 0; i < 36; i++) {
                html.append("<i style='position:absolute;left:").append((i * 37) % 100)
                        .append("%;top:-12px;width:7px;height:12px;background:")
                        .append(colors[i % colors.length]).append(";animation:fall ")
                        .append(2 + (i % 10) / 10.0).append("s ").append((i % 8) / 10.0)
                        .append("s ease-in forwards'></i>");
            }
            html.append("</div>")
                    .append("<script>(function(){try{var C=window.AudioContext||window.webkitAudioContext;if(!C)return;var c=new C();")
                    .append("var t=c.currentTime;function note(f,start,dur,wave,vol){var o=c.createOscillator(),g=c.createGain();")
                    .append("o.type=wave;o.frequency.value=f;g.gain.setValueAtTime(0.0001,t+start);g.gain.exponentialRampToValueAtTime(vol,t+start+0.015);")
                    .append("g.gain.exponentialRampToValueAtTime(0.0001,t+start+dur);o.connect(g);g.connect(c.destination);o.start(t+start);o.stop(t+start+dur+0.02);}")
                    .append("function drum(at,volume){var dur=.12,len=Math.floor(c.sampleRate*dur),buf=c.createBuffer(1,len,c.sampleRate),data=buf.getChannelData(0);")
                    .append("for(var j=0;j<len;j++){var x=j/c.sampleRate,env=Math.exp(-x*30);data[j]=(Math.random()*2-1)*env;}")
                    .append("var src=c.createBufferSource(),filter=c.createBiquadFilter(),gain=c.createGain();src.buffer=buf;filter.type='lowpass';filter.frequency.value=1800;")
                    .append("gain.gain.setValueAtTime(volume,t+at);gain.gain.exponentialRampToValueAtTime(.001,t+at+dur);src.connect(filter);filter.connect(gain);gain.connect(c.destination);src.start(t+at);}")
                    .append("var roll=0;for(var k=0;k<28;k++){drum(roll,.12+Math.min(k/28,.7)*.12);roll+=.19-k*.0045;}")
                    .append("[[523.25,roll,.22],[659.25,roll+.14,.22],[783.99,roll+.28,.22],[1046.5,roll+.42,.72],[1318.51,roll+.45,.62],[1567.98,roll+.48,.55],[2093,roll+.52,.42],[1046.5,roll+.78,.5],[1318.51,roll+.8,.48],[1567.98,roll+.82,.46]].forEach(function(n,i){note(n[0],n[1],n[2],i<6?'triangle':'sine',i<6?.2:.12);});")
                    .append("setTimeout(function(){c.close();},5500);}catch(e){}})();</script>");
        }
        int completedCount = 0; // 完了済みのTodo数を数えます。
        for (Todo todo : todos) { // Todoを順番に確認します。
            if (todo.isDone()) { // 完了済みか調べます。
                completedCount++; // 完了済みの数を1つ増やします。
            } // 完了状態の確認を終えます。
        } // 完了数の集計を終えます。
        html.append("<p>").append(todos.size()).append("件中").append(completedCount).append("件 完了</p>"); // 全件数と完了数を表示します。
        if (completedCount > 0) {
            html.append(
                    "<form method='post' action='/delete-completed' onsubmit=\"return confirm('完了済みのTodoをすべて削除します。よろしいですか？');\"><button type='submit'>完了済みを一括削除</button></form>");
        }
        if (todos.isEmpty()) { // ★ Todoが0件か確認します。
            html.append("<p>やることは、いまゼロです</p>"); // ★ 0件の案内を表示します。
        } else { // ★ Todoがある場合です。
            html.append("<ul>"); // ★ 一覧を始めます。
            for (Todo todo : todos) { // ★ Todoを1件ずつ表示します。
                String mark = todo.isDone() ? " ✔" : ""; // ★ 完了済みなら印を付けます。
                html.append("<li>").append(escapeHtml(todo.getTitle())).append(mark).append(" <a href='/done?id=") // ★
                                                                                                                   // 内容と完了リンクです。
                        .append(todo.getId()).append("'>完了</a> <a href='/delete?id=") // ★ 番号と削除リンクです。
                        .append(todo.getId()).append("'>削除</a></li>"); // ★ 一覧の行を閉じます。
            } // ★ Todo表示を終えます。
            html.append("</ul>"); // ★ 一覧を閉じます。
        } // ★ 件数ごとの表示を終えます。
        html.append("</body></html>"); // ★ HTMLを閉じます。
        return html.toString(); // ★ 完成したページを返します。
    }

    private static String escapeJson(String value) { // ★ JSON文字列内の特殊文字をエスケープします。
        StringBuilder escaped = new StringBuilder(); // ★ エスケープ後の文字列を作ります。
        for (int i = 0; i < value.length(); i++) { // ★ タイトルを1文字ずつ確認します。
            char ch = value.charAt(i); // ★ 現在の文字を取り出します。
            switch (ch) { // ★ 文字の種類に応じて処理します。
                case '"':
                    escaped.append("\\\"");
                    break; // ★ 引用符をJSON用に変換します。
                case '\\':
                    escaped.append("\\\\");
                    break; // ★ バックスラッシュをJSON用に変換します。
                case '\n':
                    escaped.append("\\n");
                    break; // ★ 改行をJSON用に変換します。
                case '\r':
                    escaped.append("\\r");
                    break; // ★ 復帰文字をJSON用に変換します。
                case '\t':
                    escaped.append("\\t");
                    break; // ★ タブをJSON用に変換します。
                default: // ★ その他の文字を確認します。
                    if (ch < 0x20) { // ★ JSONでそのまま使えない制御文字か確認します。
                        escaped.append(String.format("\\u%04x", (int) ch)); // ★ 制御文字をUnicode表記にします。
                    } else { // ★ 通常の文字の場合です。
                        escaped.append(ch); // ★ 文字をそのまま追加します。
                    } // ★ 通常文字の処理を終えます。
            } // ★ 文字ごとの処理を終えます。
        } // ★ タイトル全体の処理を終えます。
        return escaped.toString(); // ★ エスケープ済みの文字列を返します。
    } // ★ JSON用エスケープメソッドを終えます。

    private static int requestedId(String query) { // ★ URLから番号を読み取ります。
        if (query == null || !query.startsWith("id=") || query.length() <= 3) { // ★ 番号が無いか確認します。
            return -1; // ★ 不正な番号として扱います。
        } // ★ クエリ確認を終えます。
        try { // ★ 数字への変換を試します。
            return Integer.parseInt(query.substring(3)); // ★ Todo番号を返します。
        } catch (NumberFormatException e) { // ★ 数字でない場合です。
            return -1; // ★ 不正な番号として扱います。
        } // ★ 番号の変換を終えます。
    }

    public static void main(String[] args) throws Exception { // ★ アプリを起動します。
        initializeDatabase(); // ★ todos表を用意します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // ★ 8080番ポートで待ち受けます。
        server.createContext("/api/todos", exchange -> { // ★ JSON形式のTodo一覧入口を登録します。
            if (!exchange.getRequestMethod().equals("GET")) { // ★ GET要求だけ受け付けます。
                exchange.sendResponseHeaders(405, -1); // ★ 他の方法は許可しないと返します。
                exchange.close(); // ★ 通信を閉じます。
                return; // ★ この要求の処理を終えます。
            } // ★ HTTP方法の確認を終えます。
            try { // ★ 一覧取得時のDBエラーを扱います。
                StringBuilder json = new StringBuilder("["); // ★ JSON配列を作り始めます。
                List<Todo> todos = loadTodos(); // ★ SELECTで全Todoを読み込みます。
                for (int i = 0; i < todos.size(); i++) { // ★ Todoを順番にJSONへ追加します。
                    Todo todo = todos.get(i); // ★ 現在のTodoを取り出します。
                    if (i > 0) { // ★ 2件目以降か確認します。
                        json.append(","); // ★ JSON項目の間にカンマを入れます。
                    } // ★ 区切りの追加を終えます。
                    json.append("{\"title\":\"").append(escapeJson(todo.getTitle())) // ★ エスケープしたタイトルを追加します。
                            .append("\",\"done\":").append(todo.isDone()).append("}"); // ★ 完了状態をJSONの真偽値で追加します。
                } // ★ 全Todoの追加を終えます。
                json.append("]"); // ★ JSON配列を閉じます。
                byte[] response = json.toString().getBytes(StandardCharsets.UTF_8); // ★ JSONをUTF-8のバイト列にします。
                exchange.getResponseHeaders().set("Content-Type", "application/json"); // ★ charsetなしのJSON形式を指定します。
                exchange.sendResponseHeaders(200, response.length); // ★ 成功応答とデータ長を送ります。
                exchange.getResponseBody().write(response); // ★ JSONを返します。
                exchange.getResponseBody().close(); // ★ 応答を閉じます。
            } catch (SQLException e) { // ★ SQLiteの読み込み失敗を捕まえます。
                exchange.sendResponseHeaders(500, -1); // ★ サーバーエラーを返します。
                exchange.close(); // ★ 通信を閉じます。
                e.printStackTrace(); // ★ 詳細をコンソールへ表示します。
            } // ★ JSON入口の処理を終えます。
        }); // ★ JSON入口の登録を終えます.
        server.createContext("/", exchange -> { // ★ URLごとの処理を登録します。
            String path = exchange.getRequestURI().getPath(); // ★ アクセスされたパスです。
            String method = exchange.getRequestMethod(); // ★ HTTPの方法です。
            try { // ★ DB処理のエラーを扱います。
                if (path.equals("/add") && method.equals("POST")) { // ★ 追加要求です。
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // ★
                                                                                                                // フォーム内容を読みます。
                    String title = ""; // ★ 追加する内容を用意します。
                    if (body.startsWith("todo=")) { // ★ フォーム項目を確認します。
                        title = URLDecoder.decode(body.substring(5), StandardCharsets.UTF_8); // ★ 内容を元の文字に戻します。
                    } // ★ フォーム確認を終えます。
                    if (!title.isEmpty()) { // ★ 空欄でない場合です。
                        addTodo(title); // ★ INSERTでTodoを追加します。
                    } // ★ 空欄確認を終えます。
                    exchange.getResponseHeaders().set("Location", "/"); // ★ 一覧へ戻します。
                    exchange.sendResponseHeaders(303, -1); // ★ ページ移動を指示します。
                    exchange.close(); // ★ 通信を閉じます。
                    return; // ★ 追加要求を終えます。
                } else if (path.equals("/done") && method.equals("GET")) { // ★ 完了要求です。
                    int id = requestedId(exchange.getRequestURI().getQuery()); // ★ URLから番号を取得します。
                    if (id >= 0) { // ★ 有効な番号か確認します。
                        markDone(id); // ★ UPDATEで完了状態にします。
                    } // ★ 番号確認を終えます。
                    exchange.getResponseHeaders().set("Location", "/"); // ★ 一覧へ戻します。
                    exchange.sendResponseHeaders(303, -1); // ★ ページ移動を指示します。
                    exchange.close(); // ★ 通信を閉じます。
                    return; // ★ 完了要求を終えます。
                } else if (path.equals("/delete") && method.equals("GET")) { // ★ 削除要求です。
                    int id = requestedId(exchange.getRequestURI().getQuery()); // ★ URLから番号を取得します。
                    if (id >= 0) { // ★ 有効な番号か確認します。
                        deleteTodo(id); // ★ DELETEでTodoを削除します。
                    } // ★ 番号確認を終えます。
                    exchange.getResponseHeaders().set("Location", "/"); // ★ 一覧へ戻します。
                    exchange.sendResponseHeaders(303, -1); // ★ ページ移動を指示します。
                    exchange.close(); // ★ 通信を閉じます。
                    return; // ★ 削除要求を終えます。
                } else if (path.equals("/delete-completed") && method.equals("POST")) {
                    int deletedCount = deleteCompletedTodos();
                    byte[] response = renderPage(deletedCount).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.getResponseBody().close();
                    return;
                } else if (path.equals("/")) { // ★ 一覧ページの要求です。
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // ★
                                                                                                   // HTMLとUTF-8を指定します。
                    byte[] response = renderPage(0).getBytes(StandardCharsets.UTF_8); // ★ SELECT結果から画面を作ります。
                    exchange.sendResponseHeaders(200, response.length); // ★ 成功応答を始めます。
                    exchange.getResponseBody().write(response); // ★ HTMLを送ります。
                    exchange.getResponseBody().close(); // ★ 応答を閉じます。
                } else { // ★ 対応していないURLの場合です。
                    byte[] response = "ページが見つかりません".getBytes(StandardCharsets.UTF_8); // ★ 案内文をUTF-8にします。
                    exchange.sendResponseHeaders(404, response.length); // ★ 見つからない応答を返します。
                    exchange.getResponseBody().write(response); // ★ 案内文を送ります。
                    exchange.getResponseBody().close(); // ★ 応答を閉じます。
                } // ★ URL処理を終えます。
            } catch (SQLException e) { // ★ SQLite操作に失敗した場合です。
                byte[] response = "データベースの処理に失敗しました".getBytes(StandardCharsets.UTF_8); // ★ エラー案内を作ります。
                exchange.sendResponseHeaders(500, response.length); // ★ サーバーエラーを返します。
                exchange.getResponseBody().write(response); // ★ エラー案内を送ります。
                exchange.getResponseBody().close(); // ★ 応答を閉じます。
                e.printStackTrace(); // ★ 詳細をコンソールへ出します。
            } // ★ リクエスト処理を終えます。
        }); // ★ URL処理の登録を終えます。
        server.start(); // ★ サーバーを起動します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // ★ 起動先を表示します。
    } // ★ mainを終えます。
} // ★ Appクラスを終えます。
