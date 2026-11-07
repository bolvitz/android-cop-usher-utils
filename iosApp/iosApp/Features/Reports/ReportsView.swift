import SwiftUI
import shared

@MainActor
final class ReportsModel: ObservableObject {
    private let viewModel = KoinIosKt.reportsViewModel()
    private let lifetime: ViewModelLifetime
    @Published var state: ReportsUiState

    init() {
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! ReportsUiState
        lifetime.observe(viewModel.uiState, as: ReportsUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    func select(_ p: ReportPeriod) { viewModel.selectPeriod(p: p) }
    func selectVenue(_ id: String?) { viewModel.selectVenue(id: id) }
    func selectType(_ id: String?) { viewModel.selectEventType(id: id) }
    func export() { viewModel.exportCsv() }
    func clearMessage() { viewModel.clearExportMessage() }
}

struct ReportsView: View {
    @StateObject private var model = ReportsModel()
    @State private var toast: String?

    var body: some View {
        let state = model.state
        CardList(spacing: 12) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(ReportPeriod.entries, id: \.self) { p in
                        FilterChip(title: p.displayName, selected: state.period == p) { model.select(p) }
                    }
                }
            }
            HStack(spacing: 8) {
                FilterMenu(
                    label: state.venues.first { $0.id == state.selectedVenueId }?.name ?? "All venues",
                    options: [(nil, "All venues")] + state.venues.map { ($0.id, $0.name) },
                    onSelect: model.selectVenue
                )
                FilterMenu(
                    label: state.eventTypes.first { $0.id == state.selectedEventTypeId }?.name ?? "All types",
                    options: [(nil, "All types")] + state.eventTypes.map { ($0.id, $0.name) },
                    onSelect: model.selectType
                )
            }
            HStack(spacing: 8) {
                StatCard(label: "Events", value: "\(state.data.totalEvents)", large: false)
                StatCard(label: "Total", value: "\(state.data.totalAttendance)", large: false)
                StatCard(label: "Average", value: "\(state.data.averageAttendance)", large: false)
            }
            Text("By area").font(.headline).foregroundStyle(AppColor.text)
            if state.data.areaStatistics.isEmpty {
                Text("No data for the selected filters.").foregroundStyle(AppColor.secondaryText)
            } else {
                ForEach(state.data.areaStatistics, id: \.areaName) { stat in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(stat.areaName).font(.headline).foregroundStyle(AppColor.text)
                        Text("avg \(stat.averageCount) • min \(stat.minCount) • max \(stat.maxCount) • cap \(stat.capacity)")
                            .font(.caption).foregroundStyle(AppColor.secondaryText)
                    }
                    .appCard()
                }
            }
        }
        .appNavigationTitle("Reports")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: model.export) {
                    Image(systemName: "square.and.arrow.down")
                }
                .accessibilityLabel("Export CSV")
            }
        }
        .overlay(alignment: .bottom) {
            if let toast {
                Text(toast).font(.subheadline).foregroundStyle(AppColor.background)
                    .padding(.horizontal, 16).frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
                    .background(AppColor.text, in: RoundedRectangle(cornerRadius: 8))
                    .padding(16)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .onChange(of: model.state.exportMessage) { _, message in
            guard let message else { return }
            model.clearMessage()
            withAnimation { toast = message }
            UIAccessibility.post(notification: .announcement, argument: message)
        }
        // Restarts on every new toast, so an older timer can't clear a newer message.
        .task(id: toast) {
            guard toast != nil else { return }
            try? await Task.sleep(for: .seconds(4))
            guard !Task.isCancelled else { return }
            withAnimation { toast = nil }
        }
    }
}

/// Read-only outlined field that opens a menu, as Android's exposed dropdown.
private struct FilterMenu: View {
    let label: String
    let options: [(String?, String)]
    let onSelect: (String?) -> Void

    var body: some View {
        Menu {
            ForEach(Array(options.enumerated()), id: \.offset) { _, option in
                Button(option.1) { onSelect(option.0) }
            }
        } label: {
            VStack(alignment: .leading, spacing: 4) {
                Text("Filter").font(.caption).foregroundStyle(AppColor.secondaryText)
                HStack {
                    Text(label).foregroundStyle(AppColor.text).lineLimit(1)
                    Spacer()
                    Image(systemName: "arrowtriangle.down.fill").font(.system(size: 9))
                        .foregroundStyle(AppColor.secondaryText)
                }
                .padding(.horizontal, 14)
                .frame(minHeight: 52)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(AppColor.secondaryText.opacity(0.6)))
            }
        }
        .frame(maxWidth: .infinity)
    }
}
