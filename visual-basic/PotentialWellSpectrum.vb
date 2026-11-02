Option Strict On

Imports System
Imports System.Globalization

' Задача 1. Спектр энергии частицы и её волновые функции.
Public Module PotentialWellSpectrum
    Public Sub Run(args As String())
        If args.Length > 4 Then
            Throw New ArgumentException("Синтаксис: spectrum [L_нм] [уровни] [m/m_e] [x_нм]")
        End If

        Dim widthNm As Double = If(args.Length >= 1, ParseNumberArg(args, 0), 0.6)
        Dim count As Integer = If(args.Length >= 2, Integer.Parse(args(1), CultureInfo.InvariantCulture), 5)
        Dim massRatio As Double = If(args.Length >= 3, ParseNumberArg(args, 2), 1.0)
        Dim xNm As Double = If(args.Length >= 4, ParseNumberArg(args, 3), widthNm / 4.0)
        If count < 1 OrElse count > 1000 Then
            Throw New ArgumentException("Число уровней должно быть в диапазоне 1..1000")
        End If

        Positive(widthNm, "Ширина")
        Positive(massRatio, "Масса")
        If Not Double.IsFinite(xNm) Then
            Throw New ArgumentException("Координата должна быть конечным числом")
        End If

        Console.WriteLine("Задача 1. Бесконечно глубокая потенциальная яма")
        Console.WriteLine($"Ширина L = {widthNm:F6} нм; масса m = {massRatio:F6} m_e; x = {xNm:F6} нм")
        Console.WriteLine("  n |   E_n (Дж)    |  E_n (эВ)  | ψ_n(x), нм^-1/2 | |ψ_n(x)|², нм^-1")
        Console.WriteLine("----+---------------+------------+-----------------+-----------------")
        For n As Integer = 1 To count
            Dim energy As Double = EnergyJoules(n, widthNm, massRatio)
            Dim psi As Double = WaveFunction(n, xNm, widthNm)
            Dim density As Double = ProbabilityDensity(n, xNm, widthNm)
            Console.WriteLine(String.Format(CultureInfo.InvariantCulture,
                "{0,3} | {1,13:E6} | {2,10:F6} | {3,15:F6} | {4,15:F6}",
                n, energy, energy / ElectronVolt, psi, density))
        Next
    End Sub

    Private Function ParseNumberArg(args As String(), index As Integer) As Double
        Return ParseNumber(args(index))
    End Function
End Module
