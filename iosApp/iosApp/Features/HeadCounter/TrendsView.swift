import Charts
import SwiftUI
import shared

@MainActor
final class TrendsModel: ObservableObject {
    private let viewModel: TrendsViewModel
    private let lifetime: ViewModelLifetime
    @Published var state: TrendsUiState

    init(venueId: String?) {
        viewModel = KoinIosKt.trendsViewModel(venueId: venueId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! TrendsUiState
        lifetime.observe(viewModel.uiState, as: TrendsUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    func select(_ period: TrendPeriod) {
        viewModel.selectPeriod(period: period)
    }
}

private let periods: [TrendPeriod] = [.last7Days, .last30Days, .last90Days, .lastYear]

struct TrendsView: View {
    @StateObject private var model: TrendsModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var grown = false

    init(venueId: String?) {
        _model = StateObject(wrappedValue: TrendsModel(venueId: venueId))
    }

    private var state: TrendsUiState { model.state }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Picker("Period", selection: Binding(get: { state.period }, set: { model.select($0) })) {
                    ForEach(periods, id: \.self) { Text($0.label).tag($0) }
                }
                .pickerStyle(.segmented)

                if state.isLoading {
                    ProgressView().frame(maxWidth: .infinity, minHeight: 200)
                } else if state.isEmpty {
                    ContentUnavailableView(
                        "Not enough counts",
                        systemImage: "chart.bar",
                        description: Text("No counts were taken in this period. Try a longer one.")
                    )
                } else {
                    hero
                    stats
                    chart
                    if !state.eventTypeBreakdown.isEmpty { breakdown }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .foregroundStyle(CounterPalette.chalk)
        .background(CounterPalette.ground.ignoresSafeArea())
        .navigationTitle("Trends")
        .onChange(of: state.attendanceOverTime.map(\.date), initial: true) { _, _ in
            grown = false
            withAnimation(reduceMotion ? nil : .spring(response: 0.6, dampingFraction: 0.82).delay(0.05)) { grown = true }
        }
    }

    private var hero: some View {
        let growth = Int(state.growthPercentage)
        return VStack(alignment: .leading, spacing: 4) {
            Text("Average per count").font(.footnote).foregroundStyle(CounterPalette.pew)
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text("\(state.averageAttendance)")
                    .font(CounterFont.digits(76))
                    .contentTransition(.numericText(value: Double(state.averageAttendance)))
                Text("\(growth >= 0 ? "+" : "")\(growth)%")
                    .font(.headline)
                    .foregroundStyle(growth >= 0 ? CounterPalette.brass : CounterPalette.pew)
            }
            Text("Growth compares the second half of this period with the first.")
                .font(.footnote)
                .foregroundStyle(CounterPalette.pew)
        }
        .animation(.snappy, value: state.averageAttendance)
        .accessibilityElement(children: .combine)
    }

    private var stats: some View {
        HStack(spacing: 12) {
            stat("Counts", Int(state.totalEvents), CounterPalette.chalk)
            stat("Best turnout", Int(state.peakAttendance), CounterPalette.brass)
            stat("Lowest", Int(state.lowestAttendance), CounterPalette.chalk)
        }
        .padding(.vertical, 16)
        .overlay(alignment: .top) { Rectangle().fill(CounterPalette.line).frame(height: 1) }
        .overlay(alignment: .bottom) { Rectangle().fill(CounterPalette.line).frame(height: 1) }
    }

    private func stat(_ label: String, _ value: Int, _ color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.footnote).foregroundStyle(CounterPalette.pew)
            Text("\(value)")
                .font(CounterFont.digits(32))
                .foregroundStyle(color)
                .contentTransition(.numericText(value: Double(value)))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private var chart: some View {
        let peak = Float(state.peakAttendance)
        return VStack(alignment: .leading, spacing: 4) {
            Text("Attendance over time").font(.headline)
            if !state.peakEventName.isEmpty {
                Text("Best turnout: \(state.peakEventName)").font(.footnote).foregroundStyle(CounterPalette.pew)
            }
            Chart {
                ForEach(Array(state.attendanceOverTime.enumerated()), id: \.offset) { _, point in
                    BarMark(
                        x: .value("Date", Date(epochMillis: point.date), unit: .day),
                        y: .value("Attendance", grown ? point.value : 0)
                    )
                    .foregroundStyle(point.value == peak ? CounterPalette.brass : CounterPalette.plateHigh)
                    .clipShape(UnevenRoundedRectangle(topLeadingRadius: 4, topTrailingRadius: 4))
                    .accessibilityLabel(point.eventName)
                    .accessibilityValue("\(Int(point.value)) on \(point.label)")
                }
                RuleMark(y: .value("Average", grown ? Double(state.averageAttendance) : 0))
                    .foregroundStyle(CounterPalette.pew)
                    .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 3]))
                    .accessibilityHidden(true)
            }
            .chartYScale(domain: 0...(Double(max(peak, 1)) * 1.08))
            .chartYAxis {
                AxisMarks(position: .leading) { _ in
                    AxisGridLine().foregroundStyle(CounterPalette.line)
                    AxisValueLabel().foregroundStyle(CounterPalette.pew)
                }
            }
            .chartXAxis {
                AxisMarks { _ in AxisValueLabel().foregroundStyle(CounterPalette.pew) }
            }
            .frame(height: 220)
            .padding(.top, 10)
            Text("Dashed line: average").font(.caption).foregroundStyle(CounterPalette.pew)
        }
    }

    private var breakdown: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("By event type").font(.headline).padding(.bottom, 6)
            ForEach(state.eventTypeBreakdown, id: \.typeName) { row in
                HStack(alignment: .firstTextBaseline) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.typeName).font(.body.weight(.medium))
                        Text(row.count == 1 ? "1 count, average" : "\(row.count) counts, average")
                            .font(.footnote)
                            .foregroundStyle(CounterPalette.pew)
                    }
                    Spacer()
                    Text("\(row.avgAttendance)").font(CounterFont.digits(26))
                }
                .padding(.vertical, 12)
                .overlay(alignment: .bottom) { Rectangle().fill(CounterPalette.line).frame(height: 1) }
                .accessibilityElement(children: .combine)
            }
        }
    }
}
