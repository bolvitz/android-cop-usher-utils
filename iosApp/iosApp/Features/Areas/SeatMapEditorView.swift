import SwiftUI
import shared

@MainActor
final class SeatMapEditorModel: ObservableObject {
    private let viewModel: SeatMapEditorViewModel
    private let lifetime: ViewModelLifetime
    @Published var state: SeatMapEditorUiState

    init(areaTemplateId: String) {
        viewModel = KoinIosKt.seatMapEditorViewModel(areaTemplateId: areaTemplateId)
        lifetime = ViewModelLifetime(viewModel)
        state = viewModel.uiState.value as! SeatMapEditorUiState
        lifetime.observe(viewModel.uiState, as: SeatMapEditorUiState.self) { [weak self] in
            self?.state = $0
        }
    }

    func setHasSeatMap(_ enabled: Bool) {
        viewModel.setHasSeatMap(enabled: enabled)
    }

    func addRow() {
        viewModel.addRow(seatCount: 10)
    }

    func resize(_ row: SeatRowWithSeats, by delta: Int) {
        viewModel.resizeRow(rowId: row.row.id, newSeatCount: Int32(max(0, row.seats.count + delta)))
    }

    func delete(_ row: SeatRowWithSeats) {
        viewModel.deleteRow(rowId: row.row.id)
    }
}

struct SeatMapEditorView: View {
    @StateObject private var model: SeatMapEditorModel

    init(areaTemplateId: String) {
        _model = StateObject(wrappedValue: SeatMapEditorModel(areaTemplateId: areaTemplateId))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Toggle(isOn: Binding(get: { model.state.hasSeatMap }, set: { model.setHasSeatMap($0) })) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Seat map enabled").foregroundStyle(AppColor.text)
                    Text("Count is derived from occupied seats")
                        .font(.caption)
                        .foregroundStyle(AppColor.secondaryText)
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            Divider().overlay(AppColor.outline)

            if model.state.hasSeatMap {
                Text("\(model.state.rows.count) rows • \(model.state.totalSeats) seats")
                    .font(.subheadline.weight(.medium)).foregroundStyle(AppColor.text)
                    .padding(16)
                CardList {
                    ForEach(model.state.rows, id: \.row.id) { row in
                        HStack(spacing: 12) {
                            Text(row.row.label)
                                .font(.headline).foregroundStyle(AppColor.text)
                                .frame(width: 36, height: 36)
                                .background(AppColor.cardHigh, in: RoundedRectangle(cornerRadius: AppRadius.small))
                            Text("\(row.seats.count) seats").foregroundStyle(AppColor.text)
                            Spacer()
                            tonalIcon("minus", label: "Remove seat", enabled: !row.seats.isEmpty) { model.resize(row, by: -1) }
                            tonalIcon("plus", label: "Add seat") { model.resize(row, by: 1) }
                            Button { model.delete(row) } label: {
                                Image(systemName: "trash").foregroundStyle(AppColor.text).frame(width: 40, height: 40)
                            }
                            .accessibilityLabel("Delete row")
                        }
                        .appCard()
                    }
                }
            } else {
                EmptyMessage(text: "Enable the seat map to add rows and seats.")
            }
        }
        .appBackground()
        .appNavigationTitle(model.state.areaName.isEmpty ? "Seat Map" : model.state.areaName)
        .fab("Add Row", text: "Add Row", isShown: model.state.hasSeatMap) { model.addRow() }
    }

    /// Material `FilledTonalIconButton`.
    private func tonalIcon(_ name: String, label: String, enabled: Bool = true, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: name).font(.body.weight(.semibold))
                .foregroundStyle(AppColor.text)
                .frame(width: 40, height: 40)
                .background(AppColor.cardHigh, in: Circle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.38)
        .accessibilityLabel(label)
    }
}
