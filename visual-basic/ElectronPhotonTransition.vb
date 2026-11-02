Option Strict On

Imports System

' Задача 2. Переход электрона со второго энергетического уровня на первый.
Public Module ElectronPhotonTransition
    Public Sub Run(args As String())
        If args.Length > 1 Then
            Throw New ArgumentException("Синтаксис: transition [L_нм]")
        End If
        Dim widthNm As Double = If(args.Length = 1, ParseNumberArg(args), 0.6)
        Dim t As TransitionResult = ElectronTransition(widthNm)

        Console.WriteLine("Задача 2. Электрон в потенциальном ящике: переход 2 → 1")
        Console.WriteLine($"Ширина L = {widthNm:F6} нм" & Environment.NewLine)
        Console.WriteLine($"E1 (минимальная энергия) = {t.E1Joules / ElectronVolt:F6} эВ ({t.E1Joules:E6} Дж)")
        Console.WriteLine($"E2                      = {t.E2Joules / ElectronVolt:F6} эВ ({t.E2Joules:E6} Дж)")
        Console.WriteLine($"ΔE = E2 - E1            = {t.DeltaJoules / ElectronVolt:F6} эВ ({t.DeltaJoules:E6} Дж)")
        Console.WriteLine($"Длина волны фотона λ   = {t.WavelengthNm:F6} нм")
        Console.WriteLine($"Частота фотона ν       = {t.FrequencyHz:E6} Гц")
    End Sub

    Private Function ParseNumberArg(args As String()) As Double
        Return ParseNumber(args(0))
    End Function
End Module
