using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

internal static class Program
{
    [STAThread]
    private static int Main(string[] args)
    {
        string root = AppDomain.CurrentDomain.BaseDirectory;
        if (args.Length == 1 && args[0] == "--autoteste")
        {
            var engine = new LibraryEngine(root, true);
            try { engine.StartAsync().GetAwaiter().GetResult(); File.WriteAllText(Path.Combine(root, "target", "iniciador-autoteste.txt"), "OK: executavel iniciou a API e a interface web."); return 0; }
            catch (Exception ex) { Directory.CreateDirectory(Path.Combine(root, "target")); File.WriteAllText(Path.Combine(root, "target", "iniciador-autoteste.txt"), ex.ToString()); return 1; }
            finally { engine.Stop(); }
        }
        string key;
        using (var sha = SHA256.Create()) key = BitConverter.ToString(sha.ComputeHash(Encoding.UTF8.GetBytes(root.ToUpperInvariant()))).Replace("-", "");
        bool created;
        using (var mutex = new Mutex(true, "Local\\Biblioteca-" + key, out created))
        {
            if (!created) { MessageBox.Show("O iniciador da biblioteca ja esta aberto.", "Biblioteca"); return 0; }
            Application.EnableVisualStyles(); Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new LibraryWindow(root));
        }
        return 0;
    }
}

internal sealed class LibraryWindow : Form
{
    private readonly LibraryEngine engine;
    private readonly Label status;
    private readonly Button open;
    private readonly ProgressBar progress;
    private bool closing;

    public LibraryWindow(string root)
    {
        engine = new LibraryEngine(root, false);
        Text = "Biblioteca"; ClientSize = new Size(480, 285); StartPosition = FormStartPosition.CenterScreen;
        FormBorderStyle = FormBorderStyle.FixedDialog; MaximizeBox = false; BackColor = Color.FromArgb(248, 246, 241);
        Font = new Font("Segoe UI", 10);
        Controls.Add(new Label { Text = "Biblioteca", Font = new Font("Georgia", 27), ForeColor = Color.FromArgb(114, 62, 60), Location = new Point(28, 25), AutoSize = true });
        Controls.Add(new Label { Text = "Historias em movimento.", Location = new Point(30, 79), AutoSize = true, ForeColor = Color.DimGray });
        status = new Label { Text = "Preparando a biblioteca...", Location = new Point(30, 118), Size = new Size(420, 48) }; Controls.Add(status);
        progress = new ProgressBar { Location = new Point(30, 175), Size = new Size(420, 5), Style = ProgressBarStyle.Marquee }; Controls.Add(progress);
        open = new Button { Text = "Abrir biblioteca", Enabled = false, Location = new Point(30, 211), Size = new Size(205, 40), BackColor = Color.FromArgb(114, 62, 60), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
        open.Click += delegate { OpenBrowser(); }; Controls.Add(open);
        var stop = new Button { Text = "Encerrar", Location = new Point(247, 211), Size = new Size(203, 40) }; stop.Click += delegate { Close(); }; Controls.Add(stop);
        engine.Progress = message => { if (!closing && !IsDisposed) status.Text = message; };
        FormClosing += delegate { closing = true; engine.Stop(); };
        Shown += async delegate {
            try { await engine.StartAsync(); if (closing) return; progress.Visible = false; status.Text = "Biblioteca pronta em " + engine.Url + "\nMantenha esta janela aberta enquanto utiliza o sistema."; open.Enabled = true; OpenBrowser(); }
            catch (Exception ex) { if (closing) return; progress.Visible = false; status.Text = "Nao foi possivel iniciar. Consulte logs\\iniciador.log."; MessageBox.Show(this, ex.Message, "Biblioteca", MessageBoxButtons.OK, MessageBoxIcon.Warning); }
        };
    }

    private void OpenBrowser()
    {
        try { Process.Start(new ProcessStartInfo(engine.Url) { UseShellExecute = true }); }
        catch { MessageBox.Show(this, "Abra " + engine.Url + " no navegador.", "Biblioteca"); }
    }
}

internal sealed class LibraryEngine
{
    private readonly string root;
    private readonly bool test;
    private readonly object logLock = new object();
    private Process owned;
    private bool serverRunning;
    private readonly string shutdownToken = Guid.NewGuid().ToString("N");
    private bool stopping;
    public Action<string> Progress;
    public string Url { get; private set; }

    public LibraryEngine(string root, bool test) { this.root = root; this.test = test; }
    private void Say(string message) { Log(message); if (Progress != null) Progress(message); }
    private void Log(string line)
    {
        if (line == null) return;
        lock (logLock) { Directory.CreateDirectory(Path.Combine(root, "logs")); File.AppendAllText(Path.Combine(root, "logs", "iniciador.log"), line + Environment.NewLine, Encoding.UTF8); }
    }
    private Process Start(string file, string arguments, bool server = false)
    {
        var process = new Process { StartInfo = new ProcessStartInfo(file, arguments) { WorkingDirectory = root, UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true, RedirectStandardError = true } };
        if (server) process.StartInfo.EnvironmentVariables["BIBLIOTECA_SHUTDOWN_TOKEN"] = shutdownToken;
        process.OutputDataReceived += (sender, e) => Log(e.Data); process.ErrorDataReceived += (sender, e) => Log(e.Data);
        process.Start(); owned = process; process.BeginOutputReadLine(); process.BeginErrorReadLine(); return process;
    }
    public async Task StartAsync()
    {
        if (!File.Exists(Path.Combine(root, "pom.xml")) || !File.Exists(Path.Combine(root, "mvnw.cmd"))) throw new InvalidOperationException("Mantenha o executavel dentro da pasta sistema-biblioteca, junto de pom.xml e mvnw.cmd.");
        Say("Conferindo o Java instalado...");
        var javaInfo = new ProcessStartInfo("java.exe", "-version") { UseShellExecute = false, CreateNoWindow = true, RedirectStandardError = true, RedirectStandardOutput = true };
        try {
            using (var java = Process.Start(javaInfo)) {
                string version = java.StandardError.ReadToEnd(); java.WaitForExit();
                var match = Regex.Match(version, "version\\s+\"(\\d+)");
                if (!match.Success || Int32.Parse(match.Groups[1].Value) < 21) throw new InvalidOperationException("Instale o JDK 21 ou superior e configure o Java no PATH.");
            }
        } catch (System.ComponentModel.Win32Exception) { throw new InvalidOperationException("Instale o JDK 21 ou superior e configure o Java no PATH."); }
        int port = 8082;
        if (test) { var free = new TcpListener(IPAddress.Loopback, 0); free.Start(); port = ((IPEndPoint)free.LocalEndpoint).Port; free.Stop(); }
        else {
            string setting = Environment.GetEnvironmentVariable("BIBLIOTECA_PORT");
            if (!String.IsNullOrEmpty(setting) && (!Int32.TryParse(setting, out port) || port < 1024 || port > 65535)) throw new InvalidOperationException("BIBLIOTECA_PORT deve ser uma porta entre 1024 e 65535.");
        }
        var probe = new TcpListener(IPAddress.Loopback, port);
        try { probe.Start(); } catch (SocketException) { throw new InvalidOperationException("A porta " + port + " ja esta em uso. Encerre a outra instancia ou defina BIBLIOTECA_PORT."); } finally { probe.Stop(); }
        Url = "http://127.0.0.1:" + port + "/";
        Say("Preparando o sistema. A primeira abertura pode demorar alguns minutos...");
        string command = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "cmd.exe");
        var build = Start(command, "/d /s /c \"\"" + Path.Combine(root, "mvnw.cmd") + "\" -q -DskipTests package\"");
        await Task.Run(() => build.WaitForExit());
        if (stopping) throw new OperationCanceledException();
        if (build.ExitCode != 0) throw new InvalidOperationException("A compilacao nao terminou. Confira a conexao com a internet, o JDK e o arquivo logs\\iniciador.log.");
        Say("Abrindo a biblioteca e conectando aos dados...");
        string data = test ? "./target/iniciador-teste/biblioteca" : "./data/biblioteca";
        var server = Start("java.exe", "-jar target/biblioteca-api-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=" + port + " --spring.datasource.url=jdbc:h2:file:" + data + " --spring.jpa.hibernate.ddl-auto=update --spring.sql.init.mode=never --spring.h2.console.enabled=false", true);
        serverRunning = true;
        for (int attempt = 0; attempt < 120; attempt++) {
            if (stopping) throw new OperationCanceledException();
            if (server.HasExited) throw new InvalidOperationException("A biblioteca foi encerrada durante a abertura. Consulte logs\\iniciador.log.");
            bool ready = await Task.Run(() => {
                try {
                    var request = (HttpWebRequest)WebRequest.Create(Url + "api/sistema"); request.Timeout = 1200; request.Proxy = null;
                    using (var response = request.GetResponse()) using (var reader = new StreamReader(response.GetResponseStream())) return reader.ReadToEnd().Contains("\"biblioteca\"");
                } catch (WebException) { return false; }
            });
            if (ready) {
                using (var client = new WebClient()) { client.Proxy = null; if (!client.DownloadString(Url).Contains("Biblioteca")) throw new InvalidOperationException("A interface web nao foi encontrada."); }
                Say("Biblioteca pronta para usar."); return;
            }
            await Task.Delay(1000);
        }
        Stop(); throw new InvalidOperationException("O sistema demorou mais que o esperado para abrir. Consulte logs\\iniciador.log.");
    }
    public void Stop()
    {
        stopping = true; var process = owned;
        if (process == null) return;
        try {
            if (!process.HasExited) {
                if (serverRunning) {
                    try {
                        var request = (HttpWebRequest)WebRequest.Create(Url + "api/sistema/encerrar");
                        request.Method = "POST"; request.ContentLength = 0; request.Timeout = 2500; request.Proxy = null;
                        request.Headers["X-Iniciador-Token"] = shutdownToken;
                        using (var response = request.GetResponse()) { }
                        if (process.WaitForExit(8000)) return;
                    } catch (WebException) { Log("Encerramento normal indisponivel; encerrando apenas o processo iniciado por esta janela."); }
                }
                using (var stop = Process.Start(new ProcessStartInfo(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "taskkill.exe"), "/PID " + process.Id + " /T /F") { UseShellExecute = false, CreateNoWindow = true })) stop.WaitForExit(5000);
            }
        } catch (InvalidOperationException) { } catch (System.ComponentModel.Win32Exception) { }
    }
}
