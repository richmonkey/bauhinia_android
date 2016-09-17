'use strict';

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  Image,
  ListView,
  ScrollView,
  TouchableHighlight,
  ActionSheetIOS,
  ToastAndroid,
  Navigator,
  BackAndroid,
  TextInput,
  Alert,
  View
} from 'react-native';

import NavigationBar from 'react-native-navbar';
import { NativeModules } from 'react-native';
import Spinner from 'react-native-loading-spinner-overlay';
var DialogAndroid = require('react-native-dialogs');



class GroupName extends Component {
  constructor(props) {
    super(props);
    this.state = {topic:"", visible:false};
  }

  componentDidMount() {

  }

  componentWillUnmount() {

  }


  showSpinner() {
    this.setState({visible:true});
  }

  hideSpinner() {
    this.setState({visible:false});
  }

  createGroup() {
    var users = this.props.users;
    var userIDs = [];
    for (var i = 0; i < users.length; i++) {
      userIDs.push(users[i].uid);
      users[i].member_id = users[i].uid;
    }

    if (userIDs.indexOf(this.props.uid) == -1) {
      userIDs.push(this.props.uid);
    }

    if (this.state.topic.length == 0) {
      Alert.alert(
        '',
        '名称为空',
        [
          {text: '确定'},
        ]
      )
      return;
    }

    var obj = {
      master:this.props.uid, 
      name:this.state.topic, 
      "super":false, 
      members:userIDs
    };

    var url = this.props.url + "/groups";

    this.showSpinner();
    fetch(url, {
      method:"POST",  
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        "Authorization": "Bearer " + this.props.token,
      },
      body:JSON.stringify(obj),
    }).then((response) => {
      console.log("status:", response.status);

      return response.json().then((responseJson)=>{
        this.hideSpinner();
        if (response.status == 200) {
          console.log("response json:", responseJson);
          console.log("group id:", responseJson.data.group_id);
          let groupID = responseJson.data.group_id;
          var groupCreator = NativeModules.GroupCreatorModule;
          groupCreator.finishWithGroupID('' + groupID);
        } else {
          console.log(responseJson.meta.message);
          ToastAndroid.show(responseJson.meta.message, ToastAndroid.LONG);
        }
      });

    }).catch((error) => {
      console.log("error:", error);
      this.hideSpinner();
      ToastAndroid.show('' + error, ToastAndroid.LONG)
    });
  }

  
  render() {
    console.log("render group name");
    var leftButtonConfig = {
      title: '返回',
      handler: () => {
        this.props.navigator.pop();
      }
    };

    var rightButtonConfig = {
      title: '创建',
      handler: () => {
        this.createGroup();
      }
    };
    var titleConfig = {
      title: '新建群组',
    };


    return (
      <View style={{flex:1}}>
        <NavigationBar
            statusBar={{hidden:true}}
            style={{}}
            title={titleConfig}
            leftButton={leftButtonConfig} 
            rightButton={rightButtonConfig} />

        <ScrollView style={{flex:1, backgroundColor:"#F5FCFF"}}>
          <View style={{marginTop:12}}>
            <Text style={{marginLeft:12, marginBottom:4}}>群聊名称</Text>
            <TextInput
                style={{paddingLeft:12, height: 40, backgroundColor:"white"}}
                placeholder=""
                onChangeText={(text) => this.setState({topic:text})}
                value={this.state.topic}/>
          </View>
        </ScrollView>

        <Spinner visible={this.state.visible} />
      </View>
    );
  }
  
}


var GroupCreator = React.createClass({
  getInitialState: function() {
    var rowHasChanged = function (r1, r2) {
      return r1 !== r2;
    }
    var ds = new ListView.DataSource({rowHasChanged: rowHasChanged});
    var data = this.props.users.slice();

    for (var i = 0; i < data.length; i++) {
      data[i].id = i;
    }
    return {
      data:data,
      dataSource: ds.cloneWithRows(data),
      visible:false,
    };
  },


  handleCreate: function() {
    var users = [];
    var data = this.state.data;
    for (var i = 0; i < data.length; i++) {
      let u = data[i];
      if (u.selected) {
        users.push(u);
      }
    }
    if (users.length == 0) {
      return;
    }

    var route = {index: "name", users:users};
    this.props.navigator.push(route);
  },

  handleCancel: function() {
    var groupCreator = NativeModules.GroupCreatorModule;
    groupCreator.finish();
  },

  showSpinner: function() {
    this.setState({visible:true});
  },

  hideSpinner: function() {
    this.setState({visible:false});
  },

  render: function() {
    var renderRow = (rowData) => {
      var selectImage = () => {
        if (rowData.selected) {
          return  require('./img/CellBlueSelected.png');
        } else {
          return require('./img/CellNotSelected.png');
        }
      }

      return (
        <TouchableHighlight style={styles.row} onPress={() => this.rowPressed(rowData)}
                            underlayColor='#eeeeee' >
          <View style={{flexDirection:"row", flex:1, alignItems:"center" }}>
            <Image style={{marginLeft:10}} source={selectImage()}></Image>
            <Text style={{marginLeft:10}}>{rowData.name}</Text>
          </View>
        </TouchableHighlight>
      );
    }

    var leftButtonConfig = {
      title: '取消',
      handler: this.handleCancel,
    };

    var rightButtonConfig = {
      title: '下一步',
      handler: this.handleCreate,
    };
    var titleConfig = {
      title: '添加成员',
    };

    return (
      <View style={{ flex:1, backgroundColor:"#F5FCFF" }}>
        <NavigationBar
            statusBar={{hidden:true}}
            style={{}}
            title={titleConfig}
            leftButton={leftButtonConfig} 
            rightButton={rightButtonConfig} />

        <View style={{height:1, backgroundColor:"lightgrey"}}></View>

        <ListView
            dataSource={this.state.dataSource}
            renderRow={renderRow}
        />

        <Spinner visible={this.state.visible} />
      </View>
    );
  },

  rowPressed: function(rowData) {
    var data = this.state.data;
    var ds = this.state.dataSource;
    var newData = data.slice();
    var newRow = {uid:rowData.uid, name:rowData.name, id:rowData.id, selected:!rowData.selected};
    newData[rowData.id] = newRow;
    this.setState({data:newData, dataSource:ds.cloneWithRows(newData)});
  },

});


const styles = StyleSheet.create({
  row: {
    height:50,
  },
});




class GroupCreatorIndex extends Component {
  constructor(props) {
    super(props);
  }
  
  componentDidMount() {
    var self = this;

    BackAndroid.addEventListener('hardwareBackPress', () => {
      if (self.navigator && self.navigator.getCurrentRoutes().length > 1) {
        self.navigator.pop();
        return true;
      } else {
        return false;        
      }
    });
  }

  render() {
    const routes = [
      {index: "select"},
    ];


    var self = this;
    var renderScene = function(route, navigator) {
      if (route.index == "select") {
        return <GroupCreator  {...self.props} navigator={navigator}/>
      } else if (route.index == "name") {
        return <GroupName {...self.props} users={route.users} navigator={navigator}/>
      } 
    }

    return (
      <Navigator ref={(nav) => { self.navigator = nav; }} 
                 initialRoute={routes[0]}
                 renderScene={renderScene}
                 configureScene={(route, routeStack) =>
                   Navigator.SceneConfigs.FloatFromRight}/>
    );
  }

}



AppRegistry.registerComponent('GroupCreatorIndex', () => GroupCreatorIndex);


