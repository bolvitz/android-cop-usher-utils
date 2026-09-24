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
        List {
            Section {
                Toggle(isOn: Binding(get: { model.state.hasSeatMap }, set: { model.setHasSeatMap($0) })) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Seat map enabled")
                        Text("Count is derived from occupied seats")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            if model.state.hasSeatMap {
                Section {
                    ForEach(model.state.rows, id: \.row.id) { row in
                        HStack {
                            Text(row.row.label)
                                .font(.headline)
                                .frame(width: 36, height: 36)
                                .background(Color(.secondarySystemFill), in: RoundedRectangle(cornerRadius: 8))
                            Text("\(row.seats.count) seats")
                            Spacer()
                            Stepper(
                                "Seats in row \(row.row.label)",
                                onIncrement: { model.resize(row, by: 1) },
                                onDecrement: row.seats.isEmpty ? nil : {
                                    model.resize(row, by: -1)
                                }
                            )
                            .labelsHidden()
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                model.delete(row)
                            } label: {
                                Label("Delete Row", systemImage: "trash")
                            }
                        }
                    }
                    Button {
                        model.addRow()
                    } label: {
                        Label("Add Row", systemImage: "plus")
                    }
                } header: {
                    Text("\(model.state.rows.count) rows • \(model.state.totalSeats) seats")
                }
            } else {
                Section {
                    Text("Enable the seat map to add rows and seats.")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle(model.state.areaName.isEmpty ? "Seat Map" : model.state.areaName)
    }
}
