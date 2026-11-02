Option Strict On

Imports System

' Самопроверки численных результатов: отдельный пакет тестов не нужен.
Public Module QuantumWellSelfTest
    Private Sub Check(label As String, actual As Double, expected As Double, tolerance As Double)
        If Not Double.IsFinite(actual) OrElse Math.Abs(actual - expected) > tolerance Then
            Throw New InvalidOperationException($"Ошибка проверки {label}: {actual} вместо {expected}")
        End If
        Console.WriteLine($"OK {label}: {actual:G9}")
    End Sub

    Public Sub Run()
        Dim e1 As Double = EnergyJoules(1, 0.6)
        Check("E1, эВ", e1 / ElectronVolt, 1.044528, 0.000002)
        Check("E2/E1", EnergyJoules(2, 0.6) / e1, 4.0, 1.0E-12)
        Check("E3/E1", EnergyJoules(3, 0.6) / e1, 9.0, 1.0E-12)
        Check("Зависимость от L", EnergyJoules(1, 1.2) / e1, 0.25, 1.0E-12)
        Check("Зависимость от m", EnergyJoules(1, 0.6, 2) / e1, 0.5, 1.0E-12)
        Check("ψ(0)", WaveFunction(1, 0, 0.6), 0.0, 1.0E-12)
        Check("ψ(L)", WaveFunction(1, 0.6, 0.6), 0.0, 1.0E-12)
        Check("ψ2(L/2)", WaveFunction(2, 0.3, 0.6), 0.0, 1.0E-12)

        Dim width As Double = 0.6
        Dim steps As Integer = 20000
        Dim dx As Double = width / steps
        Dim integral As Double = 0.0
        For i As Integer = 0 To steps
            Dim weight As Double = If(i = 0 OrElse i = steps, 0.5, 1.0)
            integral += weight * ProbabilityDensity(3, CDbl(i) * dx, width) * dx
        Next
        Check("Нормировка плотности", integral, 1.0, 1.0E-8)

        Dim result As TransitionResult = ElectronTransition(0.6)
        Check("ΔE, эВ", result.DeltaJoules / ElectronVolt, 3.133585, 0.000003)
        Check("λ, нм", result.WavelengthNm, 395.663, 0.002)
        Console.WriteLine("Все проверки Visual Basic пройдены.")
    End Sub
End Module
