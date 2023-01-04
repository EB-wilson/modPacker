package modpacker.ui;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Scaling;
import mindustry.game.Saves;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;

public class SavesSelectorDialog extends BaseDialog {
  private BaseDialog dialog;
  private String searchString;
  private Table saveTable = new Table();
  private TextField searchField;

  Cons<Fi> cons;

  public SavesSelectorDialog(){
    super("@maps");

    buttons.remove();

    addCloseListener();

    shown(this::setup);
    onResize(() -> {
      if(dialog != null){
        dialog.hide();
      }
      setup();
    });
  }

  void setup(){
    buttons.clearChildren();

    searchString = null;

    if(Core.graphics.isPortrait()){
      buttons.button("@back", Icon.left, this::hide).size(210f * 2f, 64f).colspan(2);
      buttons.row();
    }else{
      buttons.button("@back", Icon.left, this::hide).size(210f, 64f);
    }

    cont.clear();

    rebuildMaps();

    ScrollPane pane = new ScrollPane(saveTable);
    pane.setFadeScrollBars(false);

    Table search = new Table();
    search.image(Icon.zoom);
    searchField = search.field("", t -> {
      searchString = t.length() > 0 ? t.toLowerCase() : null;
      rebuildMaps();
    }).maxTextLength(50).growX().get();
    searchField.setMessageText("@editor.search");

    cont.add(search).growX();
    cont.row();
    cont.add(pane).uniformX().growY();
    cont.row();
    cont.add(buttons).growX();
  }

  void rebuildMaps(){
    saveTable.clear();

    saveTable.marginRight(24);

    int maxwidth = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);
    float mapsize = 200f;
    boolean noMapsShown = true;

    int i = 0;

    Seq<Saves.SaveSlot> saves = control.saves.getSaveSlots();
    Seq<Saves.SaveSlot> sectors = saves.select(Saves.SaveSlot::isSector);
    Seq<Saves.SaveSlot> normals = saves.select(e -> !e.isSector());

    if (!normals.isEmpty()) {
      saveTable.table(t -> {
        t.add(Core.bundle.get("packer.saves")).center();
        t.row();
        t.image().color(Pal.accent).pad(0).height(4).growX().padTop(4).padBottom(4);
      }).growX().fillY();
      saveTable.row();
      for (Saves.SaveSlot save : normals) {
        if (searchString != null && !save.getName().toLowerCase().contains(searchString)) continue;

        noMapsShown = false;

        if (i % maxwidth == 0) {
          saveTable.row();
        }

        TextButton button = saveTable.button("", Styles.grayt, () -> showSaveInfo(save)).width(mapsize).pad(8).get();
        button.clearChildren();
        button.margin(9);
        button.add(save.getName()).width(mapsize - 18f).center().get().setEllipsis(true);
        button.row();
        button.image().growX().pad(4).color(Pal.gray);
        button.row();

        button.stack((save.previewTexture() == null? new Image(Tex.nomap): new Image(save.previewTexture())).setScaling(Scaling.fit)).size(mapsize - 20f);
        button.row();
        button.add(save.getPlayTime()).color(Color.gray).padTop(3);

        i++;
      }
      saveTable.row();
    }

    if (!sectors.isEmpty()) {
      saveTable.table(t -> {
        t.add(Core.bundle.get("packer.sectors")).center();
        t.row();
        t.image().color(Pal.accent).pad(0).height(4).growX().padTop(4).padBottom(4);
      }).growX().fillY();
      saveTable.row();
      for (Saves.SaveSlot save : sectors) {
        if (searchString != null && !save.getName().toLowerCase().contains(searchString)) continue;

        noMapsShown = false;

        if (i % maxwidth == 0) {
          saveTable.row();
        }

        TextButton button = saveTable.button("", Styles.grayt, () -> showSaveInfo(save)).width(mapsize).pad(8).get();
        button.clearChildren();
        button.margin(9);
        button.add(save.getName()).width(mapsize - 18f).center().get().setEllipsis(true);
        button.row();
        button.image().growX().pad(4).color(Pal.gray);
        button.row();
        button.stack((save.previewTexture() == null? new Image(Tex.nomap): new Image(save.previewTexture())).setScaling(Scaling.fit)).size(mapsize - 20f);
        button.row();
        button.add(save.getPlayTime()).color(Color.gray).padTop(3);

        i++;
      }
    }

    if(noMapsShown){
      saveTable.add("@maps.none");
    }
  }

  void showSaveInfo(Saves.SaveSlot save) {
    dialog = new BaseDialog("@editor.mapinfo");
    dialog.addCloseButton();

    float mapsize = Core.graphics.isPortrait() ? 160f : 300f;
    Table table = dialog.cont;

    table.stack((save.previewTexture() == null? new Image(Tex.nomap): new Image(save.previewTexture())).setScaling(Scaling.fit)).size(mapsize);

    table.table(Styles.black, desc -> {
      desc.top();
      Table t = new Table();
      t.margin(6);

      ScrollPane pane = new ScrollPane(t);
      desc.add(pane).grow();

      t.top();
      t.defaults().padTop(10).left();

      t.add("@editor.mapname").padRight(10).color(Color.gray).padTop(0);
      t.row();
      t.add(save.getName()).growX().wrap().padTop(2);
      t.row();
      t.add(Core.bundle.get("packer.playTime")).padRight(10).color(Color.gray);
      t.row();
      t.add(save.getPlayTime()).growX().wrap().padTop(2);
      t.row();
      t.add(Core.bundle.get("packer.date")).padRight(10).color(Color.gray);
      t.row();
      t.add(save.getDate()).growX().wrap().padTop(2);
      t.row();
      if (save.isSector()) {
        t.add(Core.bundle.get("packer.ownerPlanet")).padRight(10).color(Color.gray);
        t.row();
        t.add(save.getSector().planet.localizedName).growX().wrap().padTop(2);
        t.row();
      }
    }).height(mapsize).width(mapsize);

    table.row();

    table.button(Core.bundle.get("packer.select"), Icon.export, () -> {
      try{
        cons.get(save.file);
        dialog.hide();
        hide();
      }catch(Exception e){
        e.printStackTrace();
        ui.showErrorMessage("@error.mapnotfound");
      }
    }).fillX().height(54f).marginLeft(10);

    dialog.show();
  }

  public void show(Cons<Fi> cons){
    this.cons = cons;

    show();

    if(Core.app.isDesktop() && searchField != null){
      Core.scene.setKeyboardFocus(searchField);
    }
  }
}
