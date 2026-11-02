Option Strict On

Imports System
Imports System.Globalization
Imports System.Linq

' Точка входа: dotnet run --project visual-basic -- spectrum|transition|visualize|test
Public Module Program
    Public Sub Main(args As String())
        CultureInfo.CurrentCulture = CultureInfo.InvariantCulture
        Try
            Dim command As String = If(args.Length = 0, "visualize", args(0).ToLowerInvariant())
            Dim parameters As String() = args.Skip(1).ToArray()
            Select Case command
                Case "spectrum"
                    PotentialWellSpectrum.Run(parameters)
                Case "transition"
                    ElectronPhotonTransition.Run(parameters)
                Case "visualize"
                    PotentialWellVisualizer.Run(parameters)
                Case "test"
                    If parameters.Length <> 0 Then Throw New ArgumentException("Команда test не принимает параметры")
                    QuantumWellSelfTest.Run()
                Case Else
                    Throw New ArgumentException("Команды: spectrum, transition, visualize, test")
            End Select
        Catch ex As Exception When TypeOf ex Is ArgumentException OrElse
                                    TypeOf ex Is FormatException OrElse
                                    TypeOf ex Is OverflowException OrElse
                                    TypeOf ex Is IO.IOException
            Console.Error.WriteLine("Ошибка: " & ex.Message)
            Console.Error.WriteLine("Пример: dotnet run --project visual-basic -- spectrum 0.6 5 1 0.15")
            Environment.ExitCode = 1
        End Try
    End Sub
End Module
