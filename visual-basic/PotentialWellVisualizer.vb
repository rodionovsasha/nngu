Option Strict On

Imports System
Imports System.Diagnostics
Imports System.IO
Imports System.Runtime.InteropServices

' Открывает общую автономную интерактивную страницу HTML/Canvas.
' Это работает на macOS, Windows и Linux без WinForms или сторонних библиотек.
Public Module PotentialWellVisualizer
    Public Sub Run(args As String())
        If args.Length > 1 OrElse
            (args.Length = 1 AndAlso Not String.Equals(args(0), "--no-open", StringComparison.OrdinalIgnoreCase)) Then
            Throw New ArgumentException("Синтаксис: visualize [--no-open]")
        End If

        Dim page As String = FindVisualizationPage()
        Console.WriteLine("Визуализация: " & page)
        If args.Length = 1 Then Return

        Try
            Dim startInfo As ProcessStartInfo
            If RuntimeInformation.IsOSPlatform(OSPlatform.OSX) Then
                startInfo = New ProcessStartInfo("open") With {.UseShellExecute = False}
                startInfo.ArgumentList.Add(page)
            ElseIf RuntimeInformation.IsOSPlatform(OSPlatform.Windows) Then
                startInfo = New ProcessStartInfo(page) With {.UseShellExecute = True}
            Else
                startInfo = New ProcessStartInfo("xdg-open") With {.UseShellExecute = False}
                startInfo.ArgumentList.Add(page)
            End If
            Process.Start(startInfo)
        Catch ex As Exception When TypeOf ex Is System.ComponentModel.Win32Exception OrElse
                                    TypeOf ex Is InvalidOperationException
            Console.WriteLine("Не удалось открыть браузер автоматически: " & ex.Message)
            Console.WriteLine("Откройте указанный HTML-файл вручную.")
        End Try
    End Sub

    Private Function FindVisualizationPage() As String
        Dim locations As String() = {Directory.GetCurrentDirectory(), AppContext.BaseDirectory}
        For Each location As String In locations
            Dim cursor As New DirectoryInfo(location)
            For depth As Integer = 0 To 8
                Dim candidate As String = Path.Combine(cursor.FullName, "web", "potential-well.html")
                If File.Exists(candidate) Then Return candidate
                If cursor.Parent Is Nothing Then Exit For
                cursor = cursor.Parent
            Next
        Next
        Throw New FileNotFoundException("Не найден web/potential-well.html. Запускайте приложение из папки проекта.")
    End Function
End Module
