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

    init(venueId: String?) {
        _model = StateObject(wrappedValue: TrendsModel(venueId: venueId))
    }

    var body: some View {
        List {
            Section {
                Picker("Period", selection: Binding(get: { model.state.period }, set: { model.select($0) })) {
                    ForEach(periods, id: \.self) {
                        Text($0.label).tag($0)
                    }
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            if model.state.isLoading {
                ProgressView().frame(maxWidth: .infinity)
            } else if model.state.isEmpty {
                ContentUnavailableView(
                    "Not Enough Data",
                    systemImage: "chart.bar",
                    description: Text("No head counts were recorded in this period.")
                )
            } else {
                Section {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        StatTile(label: "Events", value: "\(model.state.totalEvents)")
                        StatTile(label: "Average", value: "\(model.state.averageAttendance)")
                        StatTile(label: "Peak", value: "\(model.state.peakAttendance)")
                        StatTile(
                            label: "Growth",
                            value: "\(model.state.growthPercentage >= 0 ? "+" : "")\(model.state.growthPercentage)%"
                        )
                    }
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                }

                Section("Attendance over time") {
                    Chart(Array(model.state.attendanceOverTime.enumerated()), id: \.offset) { _, point in
                        BarMark(
                            x: .value("Date", Date(epochMillis: point.date), unit: .day),
                            y: .value("Attendance", point.value)
                        )
                            .foregroundStyle(Color.accentColor)
                            .accessibilityLabel(point.eventName)
                            .accessibilityValue("\(Int(point.value)) on \(point.label)")
                    }
                    .frame(height: 220)
                    .padding(.vertical, 8)
                }

                if !model.state.eventTypeBreakdown.isEmpty {
                    Section("By event type") {
                        ForEach(model.state.eventTypeBreakdown, id: \.typeName) { row in
                            LabeledContent(row.typeName) {
                                Text("\(row.count) events • avg \(row.avgAttendance)")
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Trends")
    }
}

private struct StatTile: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.title2.bold().monospacedDigit())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
        .accessibilityElement(children: .combine)
    }
}
