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

    func save(_ f: LostItemForm, category: String) {
        viewModel.save(description: f.description, category: category, foundZone: f.foundZone, color: f.color, brand: f.brand)
    }

    func claim(by: String, contact: String) { viewModel.claim(claimedBy: by, contact: contact, notes: "") }
    func delete() { viewModel.delete() }
}

struct LostItemForm {
    var description = "", foundZone = "", color = "", brand = ""

    init() {}

    init(_ item: LostItemDto) {
        description = item.description_
        foundZone = item.foundZone
        color = item.color
        brand = item.brand
    }
}

struct LostItemDetailView: View {
    @StateObject private var model: LostItemDetailModel
    @Environment(\.dismiss) private var dismiss
    @State private var form = LostItemForm()
    @State private var loadedId: String?
    @State private var showClaim = false
    @State private var claimedBy = ""
    @State private var contact = ""

    init(itemId: String) {
        _model = StateObject(wrappedValue: LostItemDetailModel(itemId: itemId))
    }

    var body: some View {
        Group {
            if let item = model.item {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        StatusChip(text: ItemStatus.companion.fromString(value: item.status).displayName)
                        OutlinedField(label: "Description", text: $form.description)
                        OutlinedField(label: "Found zone", text: $form.foundZone)
                        OutlinedField(label: "Colour", text: $form.color)
                        OutlinedField(label: "Brand", text: $form.brand)
                        if !item.claimedBy.isEmpty {
                            Text("Claimed by \(item.claimedBy)").foregroundStyle(AppColor.text)
                        }
                        HStack(spacing: 8) {
                            WideButton(title: "Save") { model.save(form, category: item.category) }
                            if item.status == "PENDING" {
                                WideButton(title: "Claim", filled: false) { showClaim = true }
                            }
                        }
                    }
                    .padding(16)
                }
            } else {
                LoadingView()
            }
        }
        .appBackground()
        .appNavigationTitle("Lost Item")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Delete") {
                    model.delete()
                    dismiss()
                }
            }
        }
        .alert("Claim Item", isPresented: $showClaim) {
            TextField("Claimed by", text: $claimedBy)
            TextField("Contact", text: $contact)
            Button("Cancel", role: .cancel) {}
            Button("Claim") { model.claim(by: claimedBy, contact: contact) }
                .disabled(claimedBy.trimmingCharacters(in: .whitespaces).isEmpty)
        }
        .onReceive(model.$item) { item in
            // Seed the form once; later emissions shouldn't clobber in-progress edits.
            guard let item, loadedId != item.id else { return }
            form = LostItemForm(item)
            loadedId = item.id
        }
    }
}
