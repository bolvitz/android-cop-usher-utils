import SwiftUI
import shared

@MainActor
final class LostItemDetailModel: ObservableObject {
    private let viewModel: LostItemDetailViewModel
    private let lifetime: ViewModelLifetime
    @Published var item: LostItemDto?

    init(itemId: String) {
        viewModel = KoinIosKt.lostItemDetailViewModel(itemId: itemId)
        lifetime = ViewModelLifetime(viewModel)
        item = viewModel.item.value as? LostItemDto
        lifetime.observe(viewModel.item, as: LostItemDto.self) { [weak self] in
            self?.item = $0
        }
    }

    func save(_ form: LostItemEditForm) {
        viewModel.saveDetails(
            description: form.description.trimmed,
            category: form.category,
            foundZone: form.foundZone.trimmed,
            color: form.color.trimmed,
            brand: form.brand.trimmed,
            identifyingMarks: form.identifyingMarks.trimmed,
            notes: form.notes.trimmed
        )
    }

    func updateStatus(_ status: String) { viewModel.updateStatus(status: status) }
    func claim(by: String, contact: String, notes: String) {
        viewModel.claim(claimedBy: by.trimmed, contact: contact.trimmed, notes: notes.trimmed)
    }
    func delete() { viewModel.delete() }
}

struct LostItemEditForm: Equatable {
    var description = ""
    var category = "OTHER"
    var foundZone = ""
    var color = ""
    var brand = ""
    var identifyingMarks = ""
    var notes = ""

    init() {}

    init(_ item: LostItemDto) {
        description = item.description_
        category = item.category
        foundZone = item.foundZone
        color = item.color
        brand = item.brand
        identifyingMarks = item.identifyingMarks
        notes = item.notes
    }
}

struct LostItemDetailView: View {
    @StateObject private var model: LostItemDetailModel
    @Environment(\.dismiss) private var dismiss
    @State private var form = LostItemEditForm()
    @State private var loadedId: String?
    @State private var showingClaim = false
    @State private var confirmingDelete = false

    init(itemId: String) {
        _model = StateObject(wrappedValue: LostItemDetailModel(itemId: itemId))
    }

    private var isDirty: Bool {
        guard let item = model.item else { return false }
        return form != LostItemEditForm(item)
    }

    var body: some View {
        Group {
            if let item = model.item {
                content(item)
            } else {
                ProgressView()
            }
        }
        .navigationTitle("Lost Item")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: load)
        .onChange(of: model.item?.id) { load() }
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { model.save(form) }
                    .disabled(!isDirty || form.description.trimmed.isEmpty)
            }
        }
        .sheet(isPresented: $showingClaim) {
            ClaimItemSheet { by, contact, notes in
                if isDirty { model.save(form) }
                model.claim(by: by, contact: contact, notes: notes)
            }
        }
        .confirmationDialog("Delete this item?", isPresented: $confirmingDelete, titleVisibility: .visible) {
            Button("Delete", role: .destructive) {
                model.delete()
                dismiss()
            }
        }
    }

    private func load() {
        guard let item = model.item, loadedId != item.id else { return }
        form = LostItemEditForm(item)
        loadedId = item.id
    }

    private func content(_ item: LostItemDto) -> some View {
        let status = ItemStatus.companion.fromString(value: item.status)
        return Form {
            Section("Status") {
                Picker("Status", selection: Binding(get: { item.status }, set: { model.updateStatus($0) })) {
                    ForEach(itemStatuses) { Text($0.label).tag($0.name) }
                }
                .tint(Color(hex: status.color))
                if item.status == "PENDING" {
                    Button("Mark as Claimed…") { showingClaim = true }
                }
                if !item.claimedBy.isEmpty {
                    LabeledContent("Claimed by", value: item.claimedBy)
                    if !item.claimerContact.isEmpty {
                        LabeledContent("Contact", value: item.claimerContact)
                    }
                    if item.claimedDate > 0 {
                        LabeledContent("Claimed") {
                            Text(Date(epochMillis: item.claimedDate), format: .dateTime.day().month().hour().minute())
                        }
                    }
                    if !item.verificationNotes.isEmpty {
                        LabeledContent("Verification", value: item.verificationNotes)
                    }
                }
            }
            Section("Item") {
                TextField("Description", text: $form.description)
                Picker("Category", selection: $form.category) {
                    ForEach(itemCategories) { Text($0.label).tag($0.name) }
                }
                TextField("Found in", text: $form.foundZone)
            }
            Section("Details") {
                TextField("Colour", text: $form.color)
                TextField("Brand", text: $form.brand)
                TextField("Identifying marks", text: $form.identifyingMarks)
                TextField("Notes", text: $form.notes, axis: .vertical)
                    .lineLimit(1...4)
            }
            Section {
                if !item.reportedBy.isEmpty {
                    LabeledContent("Logged by", value: item.reportedBy)
                }
                if item.foundDate > 0 {
                    LabeledContent("Found") {
                        Text(Date(epochMillis: item.foundDate), format: .dateTime.day().month().hour().minute())
                    }
                }
            }
            Section {
                Button("Delete Item", role: .destructive) { confirmingDelete = true }
            }
        }
    }
}

private struct ClaimItemSheet: View {
    let onClaim: (_ by: String, _ contact: String, _ notes: String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var by = ""
    @State private var contact = ""
    @State private var notes = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Claimed by", text: $by)
                    .textContentType(.name)
                TextField("Contact (phone or email)", text: $contact)
                    .textInputAutocapitalization(.never)
                TextField("How was ownership verified?", text: $notes, axis: .vertical)
                    .lineLimit(1...4)
            }
            .navigationTitle("Claim Item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Claim") {
                        onClaim(by, contact, notes)
                        dismiss()
                    }
                    .disabled(by.trimmed.isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}
