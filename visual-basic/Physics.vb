Option Strict On

Imports System
Imports System.Globalization

' Общая математическая модель бесконечно глубокой одномерной потенциальной ямы.
' Геометрия в нм, масса в единицах m_e, энергия внутри вычислений — в джоулях.
Public Module Physics
    Public Const Planck As Double = 6.62607015E-34          ' Дж·с
    Public Const SpeedOfLight As Double = 299792458.0       ' м/с
    Public Const ElectronMass As Double = 9.1093837139E-31 ' кг
    Public Const ElectronVolt As Double = 1.602176634E-19  ' Дж/эВ
    Public Const Nanometer As Double = 1.0E-9               ' м

    Public Function Positive(value As Double, parameterName As String) As Double
        If Not Double.IsFinite(value) OrElse value <= 0.0 Then
            Throw New ArgumentException(parameterName & " должно быть конечным положительным числом")
        End If
        Return value
    End Function

    ' Разрешённая энергия E_n = n² h² / (8 m L²), в Дж.
    Public Function EnergyJoules(level As Integer, widthNm As Double,
                                 Optional massRatio As Double = 1.0) As Double
        If level < 1 Then
            Throw New ArgumentException("Номер уровня n должен быть >= 1")
        End If
        Dim widthMeters As Double = Positive(widthNm, "Ширина") * Nanometer
        Dim massKg As Double = Positive(massRatio, "Масса") * ElectronMass
        Dim energy As Double = CDbl(level) * CDbl(level) * Planck * Planck /
                               (8.0 * massKg * widthMeters * widthMeters)
        Return Positive(energy, "Энергия")
    End Function

    Public Function EnergyEv(level As Integer, widthNm As Double,
                             Optional massRatio As Double = 1.0) As Double
        Return EnergyJoules(level, widthNm, massRatio) / ElectronVolt
    End Function

    ' Нормированная волновая функция, единицы: нм^(-1/2).
    Public Function WaveFunction(level As Integer, xNm As Double, widthNm As Double) As Double
        If level < 1 OrElse Not Double.IsFinite(xNm) Then
            Throw New ArgumentException("Некорректный номер уровня или координата")
        End If
        Positive(widthNm, "Ширина")
        If xNm <= 0.0 OrElse xNm >= widthNm Then
            Return 0.0
        End If
        Return Math.Sqrt(2.0 / widthNm) * Math.Sin(CDbl(level) * Math.PI * xNm / widthNm)
    End Function

    Public Function ProbabilityDensity(level As Integer, xNm As Double, widthNm As Double) As Double
        Dim psi As Double = WaveFunction(level, xNm, widthNm)
        Return psi * psi
    End Function

    ' Энергия испускаемого фотона равна E2 - E1.
    Public Function ElectronTransition(widthNm As Double) As TransitionResult
        Dim e1 As Double = EnergyJoules(1, widthNm)
        Dim e2 As Double = EnergyJoules(2, widthNm)
        Dim delta As Double = Positive(e2 - e1, "Разность энергий")
        Return New TransitionResult With {
            .E1Joules = e1,
            .E2Joules = e2,
            .DeltaJoules = delta,
            .WavelengthNm = Planck * SpeedOfLight / delta / Nanometer,
            .FrequencyHz = delta / Planck
        }
    End Function

    ' Допускаем десятичную точку и запятую в параметрах командной строки.
    Public Function ParseNumber(raw As String) As Double
        Return Double.Parse(raw.Replace(","c, "."c), NumberStyles.Float, CultureInfo.InvariantCulture)
    End Function
End Module

Public Structure TransitionResult
    Public Property E1Joules As Double
    Public Property E2Joules As Double
    Public Property DeltaJoules As Double
    Public Property WavelengthNm As Double
    Public Property FrequencyHz As Double
End Structure
